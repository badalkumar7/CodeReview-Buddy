package com.codereview.model;

/** Describes one rule violation detected in a Java source file. */
public class CodeIssue {
    public enum Severity { HIGH, MEDIUM, LOW }
    private final String fileName, ruleName, message, suggestion;
    private final int lineNumber;
    private final Severity severity;
    public CodeIssue(String fileName, int lineNumber, String ruleName, Severity severity, String message, String suggestion) {
        this.fileName=fileName; this.lineNumber=lineNumber; this.ruleName=ruleName; this.severity=severity; this.message=message; this.suggestion=suggestion;
    }
    /** Returns the source file associated with this issue. */
    public String getFileName(){return fileName;}
    /** Returns the one-based source line where the issue begins. */
    public int getLineNumber(){return lineNumber;}
    /** Returns the name of the rule that reported this issue. */
    public String getRuleName(){return ruleName;}
    /** Returns the issue severity. */
    public Severity getSeverity(){return severity;}
    /** Returns a human-readable explanation of the issue. */
    public String getMessage(){return message;}
    /** Returns a suggested remediation. */
    public String getSuggestion(){return suggestion;}
    /** Returns a compact display string for this issue. */
    @Override public String toString(){return severity+" · "+ruleName+" · "+fileName+":"+lineNumber+" — "+message;}
}
