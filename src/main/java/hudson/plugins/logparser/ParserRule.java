package hudson.plugins.logparser;

import hudson.Extension;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

public class ParserRule extends AbstractDescribableImpl<ParserRule> {

    private String name = null;
    private String rule = null;

    public ParserRule() {
        // Empty constructor
    }

    @DataBoundConstructor
    public ParserRule(final String name, final String rule) {
        this.name = name;
        this.rule = rule;
    }

    public String getName() {
        return name;
    }

    public String getRule() {
        return rule;
    }

    @DataBoundSetter
    public void setName(final String name) {
        this.name = name;
    }

    @DataBoundSetter
    public void setRule(final String path) {
        this.rule = rule;
    }

    @Extension @Symbol("rule")
    public static class DescriptorImpl extends Descriptor<ParserRule> {
    }
}
