import { valuesDictIsEquivalent } from './values-dict-is-equivalent';

describe('valuesDictIsEquivalent', () => {
    it('undefined inputs should be equivalent', () => {
        void expect(valuesDictIsEquivalent(undefined, undefined)).toBe(true);
    });

    it('undefined should be equivalent to empty values', () => {
        void expect(valuesDictIsEquivalent(undefined, {})).toBe(true);
        void expect(valuesDictIsEquivalent({}, undefined)).toBe(true);
    });

    it('undefined should not be equivalent to values', () => {
        void expect(valuesDictIsEquivalent(undefined, { foo: ['bar'] })).toBe(false);
        void expect(valuesDictIsEquivalent({ foo: ['bar'] }, undefined)).toBe(false);
    });

    it('empty dicts should be equivalent', () => {
        void expect(valuesDictIsEquivalent({}, {})).toBe(true);
    });

    it('equal values should be equivalent', () => {
        void expect(valuesDictIsEquivalent({ foo: ['bar'] }, { foo: ['bar'] })).toBe(true);
    });

    it('equal values with different ordering should be equivalent', () => {
        void expect(valuesDictIsEquivalent({ foo: ['bar', 'baz'] }, { foo: ['baz', 'bar'] })).toBe(
            true,
        );
    });

    it('empty array should be equivalent to undefined property', () => {
        void expect(valuesDictIsEquivalent({ foo: [] }, {})).toBe(true);
        void expect(valuesDictIsEquivalent({}, { foo: [] })).toBe(true);
    });
});
