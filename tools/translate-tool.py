import argparse
import json
import os
import sys
import uuid
import xml.etree.ElementTree as XM

import gitlab
import requests
from gitlab import GitlabGetError
from requests.exceptions import HTTPError
from translate.storage import ts2 as tsparser

__version__ = "1.0.0"
__mstranslator_environ_key_name__ = "MSTRANSLATOR_API_KEY"
_gitlab_project_token = "GITLAB_PROJECT_TOKEN"
__gitlab_project_url = 'https://gitlab.int.windscribe.com'
__project_id = 414


class TranslationUnit:
    source: str
    target: str = None
    note: str = None


# android uses two letter language code , iOS and desktop use 4 letter language codes.
android_language_code_mappings = {'ar': 'ar-eg', 'de': 'de-de', 'es': 'es-es', 'fa': 'fa-ir', 'fr': 'fr-fr',
                                  'hi': 'hi-in',
                                  'in': 'id-id', 'it': 'it-it',
                                  'ja': 'ja-jp', 'ko': 'ko-kr', 'pl': 'pl-pl', 'pt': 'pt-pt', 'ru': 'ru-ru',
                                  'tr': 'tr-tr',
                                  'uk': 'uk-ua', 'vi': 'vi-vn',
                                  'zh': 'zh-cn', 'zh-rTW': 'zh-rtw',
                                  }


def has_tag(element: XM.Element, text: str) -> bool:
    splits = element.tag.split("}")
    if len(splits) > 1:
        return splits[1] == text
    return False


def parent_is_group(element: XM.Element):
    # Qt trans_unit is nested in a group
    if element is None:
        return False
    splits = element.tag.split("}")
    if len(splits) > 1:
        return splits[1] == "group"
    return False


def get_translation_units(source_file: str) -> list[TranslationUnit]:
    translation_unit_data: list[TranslationUnit] = list()
    local_file = XM.parse(source_file)
    trans_unit_path = "./*/*/"
    if parent_is_group(local_file.getroot().find("./*/*/*")):
        trans_unit_path = "./*/*/*/"
    for i in local_file.getroot().findall(trans_unit_path):
        if has_tag(i, "trans-unit"):
            unit = TranslationUnit()
            size = len(i)
            if size > 0:
                unit.source = i[0].text
            if size > 1 and has_tag(i[1], "target"):
                unit.target = i[1].text
            if size > 2 and has_tag(i[2], "note"):
                unit.note = i[2].text
            translation_unit_data.append(unit)
    return translation_unit_data


def key_exists(key: str, units: list[TranslationUnit]) -> bool:
    exists = False
    for i in units:
        if i.source == key:
            exists = True
            break
    return exists


def write_to_temp_language_file(units: [TranslationUnit], temp_language_file: str):
    print("Updating temporary language file.")
    XM.register_namespace("", "urn:oasis:names:tc:xliff:document:1.2")
    temp_file = XM.parse(temp_language_file)
    body_tag = temp_file.getroot().find("./*/")
    already_written: [TranslationUnit] = list()
    for u in units:
        key = u.source
        if key_exists(key, already_written) is False:
            unit = XM.SubElement(body_tag, "trans-unit")
            source = XM.SubElement(unit, "source")
            source.text = key
            already_written.append(u)
            if u.target is not None:
                target = XM.SubElement(unit, "target")
                target.text = u.target
            if u.note is not None:
                note = XM.SubElement(unit, "note")
                note.text = u.note
    XM.indent(temp_file.getroot())
    temp_file.write(temp_language_file, xml_declaration=True, encoding="unicode", default_namespace=None)


def simulate(source_file):
    tsfile = tsparser.tsfile.parsefile(source_file)

    target_language = tsfile.gettargetlanguage()
    if target_language is None:
        print("WARNING: the target language is missing from the ts file's header: " + source_file)
    else:
        print(f"Target language: {target_language}")

    strings_to_translate = 0
    for unit in tsfile.units:
        if unit.istranslatable() and not unit.istranslated():
            print(unit.getlocations())
            print(unit.source)
            strings_to_translate += 1

    print(f"{strings_to_translate} strings to translate")

    if args.remove_vanished:
        strings_to_remove = 0
        for unit in tsfile.units:
            if unit.isobsolete():
                print(unit.getlocations())
                print(unit.source)
                strings_to_remove += 1

        print(f"{strings_to_remove} strings to remove")


def remove_vanished(source_file):
    tsfile = tsparser.tsfile.parsefile(source_file)

    units_to_remove = []

    for unit in tsfile.units:
        if unit.isobsolete():
            units_to_remove.append(unit)

    if len(units_to_remove) == 0:
        print("There are no vanished translation units requiring removal.")
        return

    for unit in units_to_remove:
        tsfile.removeunit(unit)

    if args.output:
        if not os.path.exists(args.output):
            os.makedirs(args.output)

        tsfile.savefile(os.path.join(args.output, os.path.basename(source_file)))
    else:
        tsfile.savefile(source_file)

    print("{} vanished units removed".format(len(units_to_remove)))


def translate_ts_file(source_file, override_lang):
    print("Parsing .ts file.")
    ts_file = tsparser.tsfile.parsefile(source_file)
    target_language = ts_file.gettargetlanguage()
    if target_language is None:
        raise IOError("The target language is missing from the ts file's header: " + source_file)
    if override_lang is not None:
        target_language = override_lang
    temp_language_file_path = target_language + '.xlf'
    load_temp_language_file_from_remote(target_language)
    remote_translation_units = get_translation_units(temp_language_file_path)
    local_required_translations = []
    for unit in ts_file.units:
        if not key_exists(unit.source, remote_translation_units):
            tr_unit = TranslationUnit()
            tr_unit.source = unit.source
            tr_unit.target = unit.target
            local_required_translations.append(tr_unit)
    remote_update_required = False
    if len(local_required_translations) > 0:
        remote_update_required = True
        write_to_temp_language_file(local_required_translations, temp_language_file_path)
        remote_translation_units = update_temp_language_file_from_ml(temp_language_file_path, target_language)
    for unit in ts_file.units:
        if unit.istranslatable() and not unit.istranslated():
            for m in remote_translation_units:
                if m.source == unit.source:
                    unit.target = m.target
    print("Saving .ts file.")
    ts_file.savefile(source_file)
    if remote_update_required:
        upload_temp_language_file_to_remote(target_language)


def unit_from_element(e: XM.Element):
    tag = e.tag.split("}")[1]
    if tag != "trans-unit":
        return None
    index = 0
    unit = TranslationUnit()
    for _ in e:
        if has_tag(e[index], "source"):
            unit.source = e[index].text
        elif has_tag(e[index], "target"):
            unit.target = e[index].text
            if unit.target == unit.source:
                unit.target = None
        elif has_tag(e[index], "note"):
            unit.note = e[index].text
        index = index + 1
    return unit


def read_local_file(source_path: str):
    strings_dict = {}
    elements = XM.parse(source_path).getroot().findall("./*/*/*")
    for e in elements:
        unit = unit_from_element(e)
        if unit is not None:
            strings_dict[unit.source] = unit
    return strings_dict


def read_temp_language_file(source_path: str):
    strings_dict = {}
    elements = XM.parse(source_path).getroot().findall("./*/*/*")
    for e in elements:
        unit = unit_from_element(e)
        if unit is not None and unit.target is not None:
            strings_dict[unit.source] = unit
    return strings_dict


def translate_xlf_file(source_file, target_language: str):
    print("Parsing .xlf file. " + target_language)
    load_temp_language_file_from_remote(target_language)
    local_strings = read_local_file(source_file)
    temp_language_file_path = target_language + '.xlf'
    remote_strings = read_temp_language_file(temp_language_file_path)
    un_translated = []
    for k, v in local_strings.items():
        if v.target is None:
            un_translated.append(k)
    for k in remote_strings:
        if un_translated.__contains__(k):
            un_translated.remove(k)
    local_required_translations = []
    for string in un_translated:
        translation_unit = TranslationUnit()
        translation_unit.source = local_strings[string].source
        translation_unit.target = ""
        local_required_translations.append(translation_unit)
    remote_update_required = False
    if len(local_required_translations) > 0:
        remote_update_required = True
        write_to_temp_language_file(local_required_translations, target_language + '.xlf')
        update_temp_language_file_from_ml(target_language + '.xlf', target_language)
        remote_strings = read_temp_language_file(temp_language_file_path)
    XM.register_namespace("", "urn:oasis:names:tc:xliff:document:1.2")
    local_file = XM.parse(source_file)
    local_elements = local_file.getroot().findall("./*/*/")
    for e in local_elements:
        index = 0
        found = False
        tag = e.tag.split("}")[1]
        if tag != "trans-unit":
            continue
        if not remote_strings.keys().__contains__(e[0].text):
            continue
        for _ in e:
            if has_tag(e[index], "target"):
                e[index].text = remote_strings[e[0].text].target
                found = True
            index = index + 1
        if not found:
            target = XM.SubElement(e, "target")
            target.text = remote_strings[e[0].text].target
    XM.indent(local_file.getroot())
    print("Saving .xlf file.")
    local_file.write(source_file, xml_declaration=True, encoding="unicode", default_namespace=None)
    if remote_update_required:
        upload_temp_language_file_to_remote(target_language)


def key_from_value(dct, value):
    keys = [key for key in dct if (dct[key] == value)]
    if len(keys) > 0:
        return keys[0]
    else:
        return None


def translate_xml_file(source_file, default_android_string_file: str, target_language: str):
    load_temp_language_file_from_remote(target_language)
    local_file, english_file, remote_file = (
        XM.parse(source_file),
        XM.parse(default_android_string_file),
        XM.parse(target_language + '.xlf'),
    )
    local_strings = {elem.attrib['name']: elem.text for elem in local_file.getroot()}
    english_strings = {elem.attrib['name']: elem.text for elem in english_file.getroot()}
    remote_strings = {key_from_value(english_strings, elem[0].text): elem[0].text for elem in
                      remote_file.getroot().findall("./*/*/*")}
    missing_strings = set(english_strings).difference(local_strings)
    un_translated_strings = missing_strings.difference(remote_strings)
    local_required_translations = []
    for string in un_translated_strings:
        translation_unit = TranslationUnit()
        translation_unit.source = english_strings[string]
        translation_unit.target = ""
        local_required_translations.append(translation_unit)
    remote_update_required = False
    if len(local_required_translations) > 0:
        remote_update_required = True
        write_to_temp_language_file(local_required_translations, target_language + '.xlf')
        update_temp_language_file_from_ml(target_language + '.xlf', target_language)
    updated_file = XM.parse(target_language + '.xlf')
    updated_elements = {key_from_value(english_strings, elem[0].text): elem[1].text for elem in
                        updated_file.getroot().findall("./*/*/*")}
    string_to_write = set(updated_elements).difference(local_strings)
    for missing_string in missing_strings:
        string_to_write.add(missing_string)
    parent = local_file.getroot().find('./resources')
    if parent is None:
        parent = local_file.getroot()
    for string in string_to_write:
        if string is not None and updated_elements.get(string) is not None:
            string_tag = XM.SubElement(parent, "string")
            string_tag.set("name", string)
            string_tag.text = updated_elements.get(string)
    XM.indent(local_file.getroot())
    local_file.write(source_file, xml_declaration=True, encoding="unicode", default_namespace=None)
    if remote_update_required:
        upload_temp_language_file_to_remote(target_language)


def load_temp_language_file_from_remote(target_language: str):
    print("Pulling remote language file.")
    master_file_path = target_language + ".xlf"
    print(master_file_path.lower())
    gl = gitlab.Gitlab(url=__gitlab_project_url, private_token=os.environ.get(_gitlab_project_token))
    project = gl.projects.get(__project_id)
    remote_file = project.files.get(file_path=master_file_path.lower(), ref='master')
    local_file = open(master_file_path, 'w')
    local_file.write(remote_file.decode().decode(encoding="utf-8"))


def remote_language_support(languages: [str]):
    supported = {}
    gl = gitlab.Gitlab(url=__gitlab_project_url, private_token=os.environ.get(_gitlab_project_token))
    project = gl.projects.get(__project_id)
    for i in languages:
        master_file_path = i + ".xlf"
        try:
            project.files.get(file_path=master_file_path.lower(), ref='master')
            supported[i] = True
        except GitlabGetError:
            supported[i] = False
    return supported


def update_temp_language_file_from_ml(temp_file_path: str, target_language) -> list[TranslationUnit]:
    print("Translating from ML.")
    required_translations = []
    master_translation_units = get_translation_units(temp_file_path)
    for i in master_translation_units:
        if i.source is not None and i.target is None:
            required_translations.append(i)
    print("Translating {0}".format(len(required_translations)))
    if len(required_translations) > 0:
        updates_units = translate_from_ml(required_translations, target_language)
        edit_temp_language_file(updates_units, temp_file_path)
    return get_translation_units(temp_file_path)


def upload_temp_language_file_to_remote(target_lang: str):
    content = open(target_lang + '.xlf', 'r').read()
    print("Updating remote language file.")
    gl = gitlab.Gitlab(url=__gitlab_project_url, private_token=os.environ.get(_gitlab_project_token))
    project = gl.projects.get(__project_id)
    file = project.files.get(file_path=target_lang.lower() + '.xlf', ref='master')
    file.content = content
    file.save("develop", "{0}.xlf updated".format(target_lang.lower()))
    print("Removing temporary language file.")
    os.remove(target_lang + '.xlf')


def edit_temp_language_file(units: [TranslationUnit], master_xlf_path: str):
    XM.register_namespace("", "urn:oasis:names:tc:xliff:document:1.2")
    xml_file = XM.parse(master_xlf_path)
    t_units = xml_file.getroot().find("./*/*")
    written = 0
    for i in units:
        for t in t_units:
            if has_tag(t, "trans-unit") and i.source == t[0].text:
                written = written + 1
                r = 0
                target_found = False
                for _ in t:
                    splits = t[r].tag.split("}")
                    if len(splits) > 1:
                        tag = splits[1]
                    else:
                        tag = splits[0]
                    if tag == "target":
                        target_found = True
                        break
                    r = r + 1
                if target_found:
                    t[r].text = i.target
                else:
                    target = XM.SubElement(t, "target")
                    target.text = i.target
                break
    print("Writing to master {0}".format(written))
    XM.indent(xml_file.getroot())
    print(master_xlf_path)
    xml_file.write(master_xlf_path, xml_declaration=True, encoding="unicode", default_namespace=None)


def translate_from_ml(units_to_translate: [TranslationUnit], target_language) -> list[TranslationUnit]:
    body: dict[str:str] = []
    for i in units_to_translate:
        if i.source is not None:
            body.append({'text': i.source})

    constructed_url = "https://api.cognitive.microsofttranslator.com/translate?api-version=3.0&from=en&to=" + target_language
    header_items = {
        "Ocp-Apim-Subscription-Key": os.getenv(__mstranslator_environ_key_name__),
        "Content-type": "application/json",
        "X-ClientTraceId": str(uuid.uuid4())
    }
    request_result = requests.post(constructed_url, headers=header_items, json=body)
    request_result.raise_for_status()
    response_json = request_result.json()
    index = 0
    for _ in body:
        value: str = response_json[index]["translations"][0]["text"]
        # android hates unescaped apostrophes
        if "'" in value:
            value = value.replace("'", "\\")
        units_to_translate[index].target = value
        index = index + 1
    if args.output:
        if not os.path.exists(args.output):
            os.makedirs(args.output)
        with open(os.path.join(args.output, f"mstranslator_{os.path.basename(path)}.json"), "w") as json_file:
            json.dump(response_json, json_file)
    return units_to_translate


def translate_android_dir(source_dir: str):
    language_codes = []
    remote_language_codes = []
    default_android_file = ""
    ignore_dirs = ["values-ldrtl"]
    for d in os.scandir(source_dir):
        if not ignore_dirs.__contains__(d.name):
            if d.name.startswith("values-"):
                code = d.name[7:len(d.name)]
                language_codes.append(code)
            elif d.name.startswith("values"):
                default_android_file = source_dir + "/" + d.name + "/strings.xml"
    for i in language_codes:
        remote_language_codes.append(android_language_code_mappings.get(i))
    supported = remote_language_support(remote_language_codes)
    for code in language_codes:
        remote_code = android_language_code_mappings.get(code)
        if supported.get(remote_code):
            source_file = source_dir + "/values-" + code + "/strings.xml"
            print("Translating language code: {0}".format(code))
            if remote_code is None:
                print("Failed to map this code {0}".format(code))
                return
            translate_xml_file(source_file, default_android_file, remote_code)
            print("Successfully translated language code: {0}\n".format(code))
        else:
            print("Language is not supported. add {0}.xlf to master".format(code))


def translate_ios_dir(source_dir: str):
    language_codes = []
    for i in os.scandir(source_dir):
        if os.path.isdir(i):
            if i.name != "en.xcloc":
                language_codes.append(i.name.split(".")[0])
    supported = remote_language_support(language_codes)
    for code in language_codes:
        print(code)
        if supported.get(code):
            source_file = "{0}/{1}.xcloc/Localized Contents/{2}.xliff".format(source_dir, code, code)
            print("Translating language code: {0}".format(code))
            translate_xlf_file(source_file, code)
            print("Successfully translated language code: {0}\n".format(code))
        else:
            print("Language is not supported. add {0}.xlf to master".format(code))


def translate_desktop_dir(source_dir: str):
    for i in os.scandir(source_dir):
        print("Translating {0}".format(i.path))
        translate_ts_file(i.path, None)


def handle_dir_input(source_dir):
    for i in os.scandir(source_dir):
        if i.name.endswith("values"):
            translate_android_dir(path)
            break
        elif i.name.endswith(".xcloc"):
            translate_ios_dir(path)
            break
        elif i.name.endswith(".ts"):
            translate_desktop_dir(path)
            break


def remove_from_android_local_dir(source_dir: str, values_to_remove: list[str]):
    for code, _ in android_language_code_mappings.items():
        file_path = "{0}/values-{1}/strings.xml".format(source_dir, code)
        english_file = XM.parse("{0}/values/strings.xml".format(source_dir))
        english_strings = {elem.attrib['name']: elem.text for elem in english_file.getroot()}
        local_file = XM.parse(file_path)
        elements = local_file.getroot().findall("./")
        resource_tag = local_file.getroot()
        for i in elements:
            for v in values_to_remove:
                key_to_delete = key_from_value(english_strings, v)
                if i.get("name") == key_to_delete:
                    resource_tag.remove(i)
        XM.register_namespace("", "urn:oasis:names:tc:xliff:document:1.2")
        XM.indent(local_file.getroot())
        local_file.write(file_path, xml_declaration=True, encoding="unicode", default_namespace=None)


def remove_values_from_remote(values_to_remove: list[str]):
    for local_code, language_code in android_language_code_mappings.items():
        temp_file_path = "{0}.xlf".format(language_code)
        load_temp_language_file_from_remote(language_code)
        xml_file = XM.parse(temp_file_path)
        elements = xml_file.getroot().findall("./*/*/*")
        body = xml_file.getroot().find("./*/*")
        for i in elements:
            if has_tag(i, "trans-unit"):
                if values_to_remove.__contains__(i[0].text):
                    body.remove(i)
        XM.register_namespace("", "urn:oasis:names:tc:xliff:document:1.2")
        XM.indent(xml_file.getroot())
        xml_file.write(temp_file_path, xml_declaration=True, encoding="unicode", default_namespace=None)
        upload_temp_language_file_to_remote(language_code)


def handle_file_input(file_path):
    if file_path.endswith(".xml"):
        translate_xml_file(file_path, args.android_default_string_file, args.language, )
    elif file_path.endswith(".xlf"):
        translate_xlf_file(file_path, args.language)
    elif file_path.endswith(".ts"):
        translate_ts_file(file_path, args.language)


if __name__ == "__main__":  # pragma: no cover
    parser = argparse.ArgumentParser(
        description="Translate 'unfinished' entries in a Qt ts file to the target language specified in the file")
    parser.add_argument('--source', metavar='SOURCE', type=str, action='store', help="The Qt ts file to process.")
    parser.add_argument('--output', metavar='DIR', type=str, action='store',
                        help="Output modified file, and translator json response if translating, to this directory.  "
                             "Default is to overwrite the SOURCE file.")
    parser.add_argument('--remove-vanished', default=False, action='store_true',
                        help="Remove all 'vanished' entries from the ts file.")
    parser.add_argument('--simulate', default=False, action='store_true',
                        help="Print entries requiring translation/removal, but do not translate/remove them.")
    parser.add_argument('--language', type=str, action='store',
                        help="Override target language.")
    parser.add_argument('--android_default_string_file', type=str, action='store',
                        help="Android string file to match ids.")
    parser.add_argument('--remove', nargs='*', default=[], help="Removes values from all language files.")
    parser.add_argument('--remove-android-local-values', nargs='*', default=[],
                        help="Removes matching values  from android language files.")
    args = parser.parse_args()

    try:
        if args.remove:
            remove_values_from_remote(args.remove)
        if args.remove_android_local_values:
            path = os.path.abspath(args.source)
            remove_from_android_local_dir(path, args.remove_android_local_values)
        elif args.simulate:
            path = os.path.abspath(args.source)
            simulate(path)
        elif args.remove_vanished:
            path = os.path.abspath(args.source)
            remove_vanished(path)
        else:
            if __mstranslator_environ_key_name__ not in os.environ:
                raise IOError("Please set the '{}' environment variable to your API key.".format(
                    __mstranslator_environ_key_name__))
            path = os.path.abspath(args.source)
            if args.source:
                path = os.path.abspath(args.source)
            if os.path.isfile(path):
                handle_file_input(path)
            elif os.path.isdir(path):
                handle_dir_input(path)
    except HTTPError as http_err:
        print(f'HTTP error occurred: {http_err}')
    except IOError as io_err:
        print(io_err)
        sys.exit(1)

    sys.exit(0)