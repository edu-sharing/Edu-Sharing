package org.edu_sharing.service.password;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.apache.commons.lang.StringUtils;
import org.edu_sharing.service.util.ViolationUtils;
import org.passay.*;
import org.passay.PasswordValidator;
import org.passay.dictionary.ArrayWordList;
import org.passay.dictionary.WordListDictionary;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;

public class PasswordConstraintValidator implements ConstraintValidator<ValidPassword, String> {

    public static final String PASSWORD_POLICY_VIOLATION = "PasswordPolicyViolation";

    //    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    private final PasswordPolicySettings settings;

    @Autowired
    public PasswordConstraintValidator(PasswordPolicySettings settings) {
        this.settings = settings;
    }



    @Override
    public void initialize(ValidPassword constraintAnnotation) {
        ConstraintValidator.super.initialize(constraintAnnotation);
    }

    @Override
    public boolean isValid(String password, ConstraintValidatorContext constraintValidatorContext) {
        List<Rule> rules = new ArrayList<>(List.of(
                new LengthRule(settings.getMinLength(), settings.getMaxLength()),
                new CharacterRule(EnglishCharacterData.UpperCase, settings.getNumberOfLowerCaseCharacters()),
                new CharacterRule(EnglishCharacterData.LowerCase, settings.getNumberOfUpperCaseCharacters()),
//                new CharacterRule(GermanCharacterData.UpperCase, 1), // has lib internal issues
//                new CharacterRule(GermanCharacterData.LowerCase, 1), // has lib  internal issues
                new CharacterRule(EnglishCharacterData.Digit, settings.getNumberOfDigitCharacters()),
                new CharacterRule(EnglishCharacterData.Special, settings.getNumberOfSpecialCharacters()),
                new DictionarySubstringRule(new WordListDictionary(new ArrayWordList(settings.getIllegalSubstrings().toArray(new String[0])))),
                new DictionaryRule(new WordListDictionary(new ArrayWordList(settings.getIllegalPasswords().toArray(new String[0])))),
                new WhitespaceRule()));

        if(settings.getMaxLengthOfAlphabeticSequence() >= 0) {
            rules.add(new IllegalSequenceRule(EnglishSequenceData.Alphabetical, settings.getMaxLengthOfAlphabeticSequence(), false));
//            rules.add(new IllegalSequenceRule(GermanSequenceData.Alphabetical, 5, false)); // has lib internal issues
        }

        if(settings.getMaxLengthOfNumericalSequence() >= 0) {
            rules.add(new IllegalSequenceRule(EnglishSequenceData.Numerical, settings.getMaxLengthOfNumericalSequence(), false));
//            rules.add(new IllegalSequenceRule(GermanSequenceData.DEQwertz, 5, false)); // has lib internal issues
        }

        PasswordValidator validator = new PasswordValidator(rules);
        RuleResult result = validator.validate(new PasswordData(password));
        if (result.isValid()) {
            return true;
        }

        constraintValidatorContext.disableDefaultConstraintViolation();
        constraintValidatorContext.buildConstraintViolationWithTemplate(
                        ViolationUtils.createTemplateMessageObject(PASSWORD_POLICY_VIOLATION, String.join(", ", validator.getMessages(result)))
                )
                .addConstraintViolation();


        return false;
    }
}
