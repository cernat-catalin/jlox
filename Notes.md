## From book
- [DONE] Add support for C-style `/* ... */` block comments.
- Add support for C-style comma expressions (comma operator in expressions).
- [DONE] Add support for C-style ternary operator.
- Add error production rules to handle each binary operator without a left-hand operator.
- Extend the binary operator "+" so that if the left operand is a string, the right could be a number, bool, etc.
- Report a runtime error for 0 / 0
- Make it a runtime error to access a variable that has not been initialized (like Java `final`)
  - Or maybe even better, do not allow declaration without initialization
- [ ] Add support for `break` syntax (i.e. break from loop)
  - Handle invalid uses of `break`
- Add support for anonymous functions
- Make resolver report if a local variable is never used
- Make resolver more efficient. See challenge 4 in chapter 11 for more details
  - [DONE] Use indexes when resolving local variables instead of a map
  - [ ] Accessing globals by index (see notes below)
- Add support for static class methods. See Smalltalk and Ruby "metaclasses" (make LoxClass extend LoxInstance)
- Add support for _getter_ methods (they look like a field but actually compute the value on demand; see book)
- Add support for traits

## Other
- `Scanner` is kind of state based. Can we reduce that?
- Maybe don't make assignment an expression but rather a statement?
- Maybe have `final` by default and introduce a `mut`?
- Add support for `forEach` style for loops
- [DONE] Add `+=` and friends operator
- Make Lox statically typed
  - Check function arity at compile time
  - Should not be possible to _use_ variables/functions that are not declared
- Add tests for
  - `scopes` (error - re declaration in local scope). 
  - error - no `return` in top level code
  - classes + inheritance (invalid uses of inheritance + super)
- try catch
- [DONE] Add import and modules
- [DONE] Add arrays types
- [DONE] Add debug statement
- Report errors in the correct file when loading modules (and debug statement)

## Extra added features:
- Make `print` a native function
- Add `assert` native function

### Accessing globals by index
- We would need to add a linking step that passes information from the imported compilation unit to the one that imports.
Otherwise, there is no way to know in unit A in what position in the global array variable x is in unit B.