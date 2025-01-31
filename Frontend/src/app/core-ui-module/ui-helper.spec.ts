import { UIHelper } from './ui-helper';

describe('Test UI Helper', () => {
    describe('Test password strength', () => {
        it('should be weak password', async () => {
            void expect(UIHelper.getPasswordStrengthString('abc')).toBe('weak');
            void expect(UIHelper.getPasswordStrengthString('abcd')).toBe('weak');
            void expect(UIHelper.getPasswordStrengthString('abc2')).toBe('weak');
        });
        it('should be accept password', async () => {
            void expect(UIHelper.getPasswordStrengthString('abcde')).toBe('accept');
            void expect(UIHelper.getPasswordStrengthString('abcde2')).toBe('accept');
            void expect(UIHelper.getPasswordStrengthString('abcdef2')).toBe('accept');
        });
        it('should be medium password', async () => {
            void expect(UIHelper.getPasswordStrengthString('ABCabc123')).toBe('medium');
            void expect(UIHelper.getPasswordStrengthString('ABCabc12345')).toBe('medium');
            void expect(UIHelper.getPasswordStrengthString('ABCabc#!$')).toBe('medium');
        });
        it('should be strong password', async () => {
            void expect(UIHelper.getPasswordStrengthString('ABCabc123#1!')).toBe('strong');
            void expect(UIHelper.getPasswordStrengthString('ABCabc12345#23')).toBe('strong');
            void expect(UIHelper.getPasswordStrengthString('ABCabcABCabcABCabcABCabc')).toBe(
                'strong',
            );
        });
    });
});
