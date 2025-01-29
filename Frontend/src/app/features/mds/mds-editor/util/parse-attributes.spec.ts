import { MdsWidget, RequiredMode } from '../../types/types';
import { parseAttributes } from './parse-attributes';

const definitionBoilerplate: MdsWidget = {
    allowempty: false,
    bottomCaption: null,
    caption: null,
    condition: null,
    defaultMax: null,
    defaultMin: null,
    defaultvalue: null,
    hasValues: true,
    icon: null,
    id: null,
    interactionType: 'Input',
    isExtended: false,
    isRequired: RequiredMode.Optional,
    isSearchable: false,
    link: null,
    max: null,
    maxlength: 0,
    min: null,
    placeholder: null,
    step: null,
    subwidgets: null,
    template: null,
    type: null,
    unit: null,
    values: null,
    hideIfEmpty: false,
};

describe('parseAttributes', () => {
    it('should pass unmodified widget through', () => {
        const originalDefinition = {
            ...definitionBoilerplate,
            id: 'ccm:foo',
        };
        const modifiedDefinition = parseAttributes('<ccm:foo>', originalDefinition);
        void expect(modifiedDefinition).toEqual(originalDefinition);
    });

    it('should throw an error on missing widget', () => {
        const originalDefinition = {
            ...definitionBoilerplate,
            id: 'ccm:foo',
        };
        void expect(() => parseAttributes('<ccm:bar>', originalDefinition)).toThrowError(
            /widget not found .* ccm:foo/,
        );
    });

    it('should read "bottomCaption" attribute', () => {
        const originalDefinition = {
            ...definitionBoilerplate,
            id: 'ccm:foo',
        };
        const modifiedDefinition = parseAttributes(
            '<ccm:foo bottomCaption="bar">',
            originalDefinition,
        );
        void expect(modifiedDefinition).toEqual({ ...originalDefinition, bottomCaption: 'bar' });
    });

    it('should leave original definition unmodified', () => {
        const originalDefinition = {
            ...definitionBoilerplate,
            id: 'ccm:foo',
        };
        const modifiedDefinition = parseAttributes(
            '<ccm:foo bottomCaption="bar">',
            originalDefinition,
        );
        void expect(modifiedDefinition).not.toEqual(originalDefinition);
    });

    it('should handle other widgets', () => {
        const originalDefinition = {
            ...definitionBoilerplate,
            id: 'ccm:foo',
        };
        const modifiedDefinition = parseAttributes(
            '<ccm:bar bottomCaption="bar">' + '<ccm:foo bottomCaption="foo">',
            originalDefinition,
        );
        void expect(modifiedDefinition).toEqual({ ...originalDefinition, bottomCaption: 'foo' });
    });

    it('should read multiple attributes', () => {
        const originalDefinition = {
            ...definitionBoilerplate,
            id: 'ccm:foo',
        };
        const modifiedDefinition = parseAttributes(
            '<ccm:foo caption="foo" hideIfEmpty="true" maxlength="42">',
            originalDefinition,
        );
        void expect(modifiedDefinition).toEqual({
            ...originalDefinition,
            caption: 'foo',
            hideIfEmpty: true,
            maxlength: 42,
        });
    });

    it('should parse JSON', () => {
        const originalDefinition = {
            ...definitionBoilerplate,
            id: 'ccm:foo',
        };
        const modifiedDefinition = parseAttributes(
            '<ccm:foo values=\'[{"id": "foo", "caption": "Foo"}]\'>',
            originalDefinition,
        );
        void expect(modifiedDefinition).toEqual({
            ...originalDefinition,
            values: [{ id: 'foo', caption: 'Foo' }],
        });
    });

    it('should guess made up attributes', () => {
        const originalDefinition = {
            ...definitionBoilerplate,
            id: 'ccm:foo',
            madeUpNumber: 0,
            madeUpBoolean: false,
            madeUpNull: null as string,
            madeUpString: '',
        };
        const modifiedDefinition = parseAttributes(
            '<ccm:foo ' +
                'madeUpNumber="42" ' +
                'madeUpBoolean="true" ' +
                'madeUpNull="foo" ' +
                'madeUpString="bar" ' +
                'anotherstring="baz">',
            originalDefinition,
        );
        void expect(modifiedDefinition).toEqual({
            ...originalDefinition,
            madeUpNumber: 42,
            madeUpBoolean: true,
            madeUpNull: 'foo',
            madeUpString: 'bar',
            anotherstring: 'baz',
        } as MdsWidget);
    });

    it('should use known attributes', () => {
        const originalDefinition = {
            id: 'ccm:foo',
        } as MdsWidget;
        const modifiedDefinition = parseAttributes(
            '<ccm:foo caption="foo" hideIfEmpty="true" maxlength="42">',
            originalDefinition,
        );
        void expect(modifiedDefinition).toEqual({
            ...originalDefinition,
            caption: 'foo',
            hideIfEmpty: true,
            maxlength: 42,
        });
    });

    it('should map "required"', () => {
        const originalDefinition = {
            ...definitionBoilerplate,
            id: 'ccm:foo',
        };
        const modifiedDefinition = parseAttributes(
            '<ccm:foo required="mandatory">',
            originalDefinition,
        );
        void expect(modifiedDefinition).toEqual({
            ...originalDefinition,
            isRequired: RequiredMode.Mandatory,
        });
    });

    it('should map "extended"', () => {
        const originalDefinition = {
            ...definitionBoilerplate,
            id: 'ccm:foo',
        };
        const modifiedDefinition = parseAttributes('<ccm:foo extended="true">', originalDefinition);
        void expect(modifiedDefinition).toEqual({
            ...originalDefinition,
            isExtended: true,
        });
    });
});
