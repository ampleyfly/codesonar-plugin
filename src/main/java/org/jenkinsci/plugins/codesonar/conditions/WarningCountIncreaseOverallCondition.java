package org.jenkinsci.plugins.codesonar.conditions;

import hudson.Extension;
import hudson.Launcher;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.model.Result;
import hudson.util.FormValidation;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.Symbol;
import org.jenkinsci.plugins.codesonar.CodeSonarBuildAction;
import org.jenkinsci.plugins.codesonar.models.CodeSonarBuildActionDTO;
import org.jenkinsci.plugins.codesonar.models.analysis.Analysis;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;

import javax.annotation.Nonnull;

public class WarningCountIncreaseOverallCondition extends Condition {

    private static final String NAME = "Warning count increase: overall";
    private String percentage = String.valueOf(5.0f);
    private String warrantedResult = Result.UNSTABLE.toString();
    private String resultDescription = NAME;

    @DataBoundConstructor
    public WarningCountIncreaseOverallCondition(String percentage) {
        this.percentage = percentage;
    }

    /**
     * @return the percentage
     */
    public String getPercentage() {
        return percentage;
    }

    /**
     * @param percentage the percentage to set
     */
    public void setPercentage(String percentage) {
        this.percentage = percentage;
    }

    public String getWarrantedResult() {
        return warrantedResult;
    }

    @DataBoundSetter
    public void setWarrantedResult(String warrantedResult) {
        this.warrantedResult = warrantedResult;
    }

    @Override
    public Result validate(CodeSonarBuildActionDTO current, CodeSonarBuildActionDTO previous, Launcher launcher, TaskListener listener) {
        if (current == null) {
            return Result.SUCCESS;
        }        

        if (previous == null) {
            return Result.SUCCESS;
        }

        int previousCount = previous.getAnalysisActiveWarnings().getWarnings().size();
        int currentCount = current.getAnalysisActiveWarnings().getWarnings().size();
        int diff = currentCount - previousCount;
        float thresholdPercentage = Float.parseFloat(percentage);
        float result = (((float) diff) / previousCount) * 100;

        if (result > thresholdPercentage) {
            resultDescription = String.format("More than %.2f%% increase in warnings (%.2f%%, %d out of %d)", thresholdPercentage, result, diff, previousCount);
            return Result.fromString(warrantedResult);
        }

        resultDescription = String.format("At most %.2f%% increase in warnings (%.2f%%, %d out of %d)", thresholdPercentage, result, diff, previousCount);
        return Result.SUCCESS;
    }

    @Override
    public String describeResult() {
        return resultDescription;
    }

    @Symbol("warningCountIncreaseOverall")
    @Extension
    public static final class DescriptorImpl extends ConditionDescriptor<WarningCountIncreaseOverallCondition> {

        public DescriptorImpl() {
            load();
        }

        @Override
        public @Nonnull String getDisplayName() {
            return NAME;
        }

        public FormValidation doCheckPercentage(@QueryParameter("percentage") String percentage) {
            if (StringUtils.isBlank(percentage)) {
                return FormValidation.error("Cannot be empty");
            }

            try {
                float v = Float.parseFloat(percentage);

                if(v < 0) {
                    return FormValidation.error("The provided value must be zero or greater");
                }
            } catch (NumberFormatException numberFormatException) {
                return FormValidation.error("Not a valid decimal number");
            }

            return FormValidation.ok();
        }

    }
}
