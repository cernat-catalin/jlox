## From book
- Add support for C-style `/* ... */` block comments.
- Add support for C-style comma expressions (comma operator in expressions).
- Add support for C-style ternary operator.
- Add error production rules to handle each binary operator without a left-hand operator.
- Extend the binary operator "+" so that if the left operand is a string, the right could be a number, bool, etc.
- Report a runtime error for 0 / 0
- Make it a runtime error to access a variable that has not been initialized (like Java `final`)
  - Or maybe even better, do not allow declaration without initialization
- Add support for `break` syntax (i.e. break from loop)
- Add support for anonymous functions

## Other
- `Scanner` is kind of state based. Can we reduce that?
- Maybe don't make assignment an expression but rather a statement?
- Maybe have `final` by default and introduce a `mut`?
- Add support for `forEach` style for loops
- Make print a native function
- Make a native function for asserts
- Make Lox statically typed
  - Check function arity at compile time
  - Should not be possible to _use_ variables/functions that are not declared