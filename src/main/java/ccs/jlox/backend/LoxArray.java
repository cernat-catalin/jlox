package ccs.jlox.backend;

final class LoxArray {
  private final int size;
  private final Object[] array;

  LoxArray(int size) {
    this.size = size;
    this.array = new Object[size];
  }

  Object get(int index) {
    return array[index];
  }

  void set(int index, Object value) {
    array[index] = value;
  }
}
