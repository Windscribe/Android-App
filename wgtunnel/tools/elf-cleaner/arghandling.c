/* Based on function in emacs's emacs.c

   Copyright (C) 1985-1987, 1993-1995, 1997-1999, 2001-2022 Free Software
   Foundation, Inc. */

#include <stdbool.h>
#include <stddef.h>
#include <stdlib.h>
#include <string.h>

/* Test whether the next argument in ARGV matches SSTR or a prefix of
   LSTR (at least MINLEN characters).  If so, then if VALPTR is non-null
   (the argument is supposed to have a value) store in *VALPTR either
   the next argument or the portion of this one after the equal sign.
   ARGV is read starting at position *SKIPPTR; this index is advanced
   by the number of arguments used.
   Too bad we can't just use getopt for all of this, but we don't have
   enough information to do it right.  */

bool
argmatch (char **argv, int argc, const char *sstr, const char *lstr,
          int minlen, int *valptr, int *skipptr)
{
  char *p = NULL;
  ptrdiff_t arglen;
  char *arg;

  /* Don't access argv[argc]; give up in advance.  */
  if (argc <= *skipptr + 1)
    return 0;

  arg = argv[*skipptr+1];
  if (arg == NULL)
    return 0;
  if (strcmp (arg, sstr) == 0)
    {
      if (valptr != NULL)
	{
          *valptr = atoi(argv[*skipptr+2]);
	  *skipptr += 2;
	}
      else
	*skipptr += 1;
      return 1;
    }
  arglen = (valptr != NULL && (p = strchr (arg, '=')) != NULL
	    ? p - arg : strlen (arg));
  if (!lstr)
    return 0;
  if (arglen < minlen || strncmp (arg, lstr, arglen) != 0)
    return 0;
  else if (valptr == NULL)
    {
      *skipptr += 1;
      return 1;
    }
  else if (p != NULL)
    {
      *valptr = atoi(p+1);
      *skipptr += 1;
      return 1;
    }
  else if (argv[*skipptr+2] != NULL)
    {
      *valptr = atoi(argv[*skipptr+2]);
      *skipptr += 2;
      return 1;
    }
  else
    {
      return 0;
    }
}
