# Consumed by anything that embeds this module, including the fused windscribe-core AAR.
# These rules belong here rather than in an app's own file: the app modules are not fused, so a
# partner build shrinking this code would otherwise have no rules for it at all.

# Charon resolves these from native code by name.
-keep public class org.strongswan.android.** {
  public protected private *;
}
