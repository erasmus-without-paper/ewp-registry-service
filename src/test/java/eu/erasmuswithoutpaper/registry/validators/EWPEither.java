package eu.erasmuswithoutpaper.registry.validators;

import java.util.function.Consumer;
import java.util.function.Function;

public class EWPEither<L, R> {
  private L left;
  private R right;
  boolean hasLeft;

  private EWPEither() {}

  public static <L, R> EWPEither<L, R> fromLeft(L _left) {
    EWPEither<L, R> either = new EWPEither<>();
    either.left = _left;
    either.right = null;
    either.hasLeft = true;
    return either;
  }

  public static <L, R> EWPEither<L, R> fromRight(R _right) {
    EWPEither<L, R> either = new EWPEither<>();
    either.left = null;
    either.right = _right;
    either.hasLeft = false;
    return either;
  }

  public <T> T map(
      Function<? super L, ? extends T> _left_map,
      Function<? super R, ? extends T> _right_map
  ) {
    if (hasLeft) {
       return _left_map.apply(left);
    }
    else {
      return _right_map.apply(right);
    }
  }

  public void mapVoid(
      Consumer<? super L> _left_map,
      Consumer<? super R> _right_map
  ) {
    if (hasLeft) {
      _left_map.accept(left);
    }
    else {
      _right_map.accept(right);
    }
  }

  public boolean isLeft() {
    return hasLeft;
  }

  public boolean isRight() {
    return !isLeft();
  }

  public L getLeft() {
    return left;
  }

  public R getRight() {
    return right;
  }
}
