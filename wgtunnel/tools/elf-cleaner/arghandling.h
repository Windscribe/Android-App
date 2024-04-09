/* Taken from emacs */
#define ARRAYELTS(arr) (sizeof (arr) / sizeof (arr)[0])

extern "C" bool
argmatch (char **, int, const char *, const char *, int, int *, int *);
