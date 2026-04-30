package logbook.internal;

import java.util.Objects;

/**
 * Tuple
 *
 */
public sealed interface Tuple permits Tuple.Unit, Tuple.Pair, Tuple.Triplet {

    static <A> Unit<A> of(A _1) {
        return new Unit<>(_1);
    }

    static <A, B> Pair<A, B> of(A _1, B _2) {
        return new Pair<>(_1, _2);
    }

    static <A, B, C> Triplet<A, B, C> of(A _1, B _2, C _3) {
        return new Triplet<>(_1, _2, _3);
    }

    public static final class Unit<A> implements Tuple {

        private final A _1;

        protected Unit(A _1) {
            this._1 = _1;
        }

        public A get1() {
            return this._1;
        }

        public Unit<A> asUnit() {
            return this;
        }

        public <B> Pair<A, B> asPair() {
            return new Pair<>(this._1, null);
        }

        public <B, C> Triplet<A, B, C> asTriplet() {
            return new Triplet<>(this._1, null, null);
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(this._1);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj instanceof Unit<?> unit) {
                return Objects.equals(this._1, unit._1);
            }
            return false;
        }

        @Override
        public String toString() {
            return "Unit[_1=" + this._1 + "]";
        }
    }

    public static non-sealed class Pair<A, B> implements Tuple {

        private final A _1;
        private final B _2;

        protected Pair(A _1, B _2) {
            this._1 = _1;
            this._2 = _2;
        }

        public A get1() {
            return this._1;
        }

        public B get2() {
            return this._2;
        }

        public A getKey() {
            return this._1;
        }

        public B getValue() {
            return this._2;
        }

        public Unit<A> asUnit() {
            return new Unit<>(this._1);
        }

        public Pair<A, B> asPair() {
            return this;
        }

        public <C> Triplet<A, B, C> asTriplet() {
            return new Triplet<>(this._1, this._2, null);
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(this._1) + Objects.hashCode(this._2);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj instanceof Pair<?, ?> unit) {
                return Objects.equals(this._1, unit._1)
                        && Objects.equals(this._2, unit._2);
            }
            return false;
        }

        @Override
        public String toString() {
            return "Pair[_1=" + this._1 + ", _2=" + this._2 + "]";
        }
    }

    public static final class Triplet<A, B, C> implements Tuple {

        private final A _1;
        private final B _2;
        private final C _3;

        protected Triplet(A _1, B _2, C _3) {
            this._1 = _1;
            this._2 = _2;
            this._3 = _3;
        }

        public A get1() {
            return this._1;
        }

        public B get2() {
            return this._2;
        }

        public C get3() {
            return this._3;
        }

        public Unit<A> asUnit() {
            return new Unit<>(this._1);
        }

        public Pair<A, B> asPair() {
            return new Pair<>(this._1, this._2);
        }

        public Triplet<A, B, C> asTriplet() {
            return this;
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(this._1) + Objects.hashCode(this._2) + Objects.hashCode(this._3);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj instanceof Triplet<?, ?, ?> unit) {
                return Objects.equals(this._1, unit._1)
                        && Objects.equals(this._2, unit._2)
                        && Objects.equals(this._3, unit._3);
            }
            return false;
        }

        @Override
        public String toString() {
            return "Triplet[_1=" + this._1 + ", _2=" + this._2 + ", _3=" + this._3 + "]";
        }
    }
}
