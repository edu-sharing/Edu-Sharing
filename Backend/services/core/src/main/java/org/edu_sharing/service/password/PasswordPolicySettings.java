package org.edu_sharing.service.password;

import lombok.Data;
import org.edu_sharing.lightbend.ConfigurationProperties;
import org.edu_sharing.spring.scope.refresh.annotations.RefreshScope;

import java.util.ArrayList;
import java.util.List;

@Data
@RefreshScope
@ConfigurationProperties(prefix = "security.passwordPolicy")
public class PasswordPolicySettings {
    private int minLength = 1;
    private int maxLength = Integer.MAX_VALUE;
    private int numberOfLowerCaseCharacters = 0;
    private int numberOfUpperCaseCharacters = 0;
    private int numberOfDigitCharacters = 0;
    private int numberOfSpecialCharacters = 0;
    private int maxLengthOfAlphabeticSequence = Integer.MAX_VALUE;
    private int maxLengthOfNumericalSequence = Integer.MAX_VALUE;
    private List<String> illegalSubstrings = new ArrayList<>();
    private List<String> illegalPasswords = new ArrayList<>();
}
