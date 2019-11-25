package hudson.plugins.logparser;

import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.Util;
import hudson.model.Action;
import hudson.model.BuildListener;
import hudson.model.Result;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.plugins.logparser.action.LogParserProjectAction;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Publisher;
import hudson.tasks.Recorder;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import hudson.util.ListBoxModel;
import jenkins.model.Jenkins;
import jenkins.tasks.SimpleBuildStep;
import net.sf.json.JSONObject;

import org.jenkinsci.Symbol;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.StaplerRequest;

public class LogParserPublisher extends Recorder implements SimpleBuildStep, Serializable {

    private static final long serialVersionUID = 1L;
    public boolean unstableOnWarning;
    public boolean failBuildOnError;
    public boolean showGraphs;
    public String parsingRules = null;
    public boolean useProjectRule;
    public String projectRule = null;

    /**
     * Create new LogParserPublisher.
     *
     * @param unstableOnWarning mark build unstable if warnings found.
     * @param failBuildOnError  mark build failed if errors found.
     * @param showGraphs        show graphs on job page.
     * @param parsingRules      the global parsing rule.
     * @param useProjectRule    true if we use a project specific rule.
     * @param projectRule       project rule with textarea
     */
    @Deprecated
    private LogParserPublisher(final boolean unstableOnWarning,
                               final boolean failBuildOnError, final boolean showGraphs,
                               final String parsingRules, final boolean useProjectRule,
                               final String projectRule) {

        this.unstableOnWarning = unstableOnWarning;
        this.failBuildOnError = failBuildOnError;
        this.showGraphs = showGraphs;
        this.parsingRules = parsingRules;
        this.useProjectRule = useProjectRule;
        this.projectRule = projectRule;
    }

    @DataBoundConstructor
    public LogParserPublisher(boolean useProjectRule, String projectRule, String parsingRules) {
        super();
        if (useProjectRule) {
            this.projectRule = projectRule;
            this.parsingRules = null;
        } else {
            this.parsingRules = parsingRules;
            this.projectRule = null;
        }
        this.useProjectRule = useProjectRule;
    }

    @DataBoundSetter
    public void setUnstableOnWarning(boolean unstableOnWarning) {
        this.unstableOnWarning = unstableOnWarning;
    }

    @DataBoundSetter
    public void setFailBuildOnError(boolean failBuildOnError) {
        this.failBuildOnError = failBuildOnError;
    }

    @DataBoundSetter
    public void setShowGraphs(boolean showGraphs) {
        this.showGraphs = showGraphs;
    }

    @Override
    public void perform(Run<?, ?> build, FilePath workspace, Launcher launcher, TaskListener listener) throws
            InterruptedException, IOException {

        final Logger logger = Logger.getLogger(getClass().getName());
        LogParserResult result = new LogParserResult();
        try {
            // Create a parser with the parsing rules as configured : colors, regular expressions, etc.
            boolean preformattedHtml = !((DescriptorImpl) getDescriptor()).getLegacyFormatting();
            final String parsingRule;
            if (useProjectRule) {
                parsingRule = projectRule;
            } else {
                parsingRule = parsingRules;
            }
            final LogParserParser parser = new LogParserParser(parsingRule, preformattedHtml, launcher.getChannel());
            // Parse the build's log according to these rules and get the result
            result = parser.parseLog(build);

            // Mark build as failed/unstable if necessary
            if (this.failBuildOnError && result.getTotalErrors() > 0) {
                build.setResult(Result.FAILURE);
            } else if (this.unstableOnWarning && result.getTotalWarnings() > 0) {
                build.setResult(Result.UNSTABLE);
            }
        } catch (IOException e) {
            // Failure to parse should not fail the build - but should be
            // handled as a serious error.
            // This should catch all process problems during parsing, including
            // parser file not found..
            logger.log(Level.SEVERE, LogParserConsts.CANNOT_PARSE + build, e);
            result.setFailedToParseError(e.toString());
        } catch (NullPointerException e) {
            // in case the rules path is null
            logger.log(Level.SEVERE, LogParserConsts.CANNOT_PARSE + build, e);
            result.setFailedToParseError(e.toString());
            build.setResult(Result.ABORTED);
        } catch (InterruptedException e) {
            logger.log(Level.SEVERE, LogParserConsts.CANNOT_PARSE + build, e);
            result.setFailedToParseError(e.toString());
            build.setResult(Result.ABORTED);
        }

        // Add an action created with the above results
        final LogParserAction action = new LogParserAction(build, result);
        build.addAction(action);
    }

    @Override
    public BuildStepDescriptor<Publisher> getDescriptor() {
        return Jenkins.getInstance().getDescriptorByType(LogParserPublisher.DescriptorImpl.class);
    }

    @Extension @Symbol("logParser")
    public static final class DescriptorImpl extends
            BuildStepDescriptor<Publisher> {

        private List<ParserRule> parsingRulesGlobal = new ArrayList<ParserRule>();
        private boolean useLegacyFormatting = false;

        public DescriptorImpl() {
            super(LogParserPublisher.class);
            load();
        }

        @Override
        public String getDisplayName() {
            return "Console output (build log) parsing";
        }

        @Override
        public String getHelpFile() {
            return "/plugin/log-parser/help.html";
        }

        @Override
        public boolean isApplicable(
                final Class<? extends AbstractProject> jobType) {
            return true;
        }

        public List<ParserRule> getParsingRulesGlobal() {
            return parsingRulesGlobal;
        }

        @DataBoundSetter
        public void setParsingRulesGlobal(List<ParserRule> parsingRulesChoices) {
            this.parsingRulesGlobal = parsingRulesChoices;
        }

        public boolean getLegacyFormatting() {
            return useLegacyFormatting;
        }

        @DataBoundSetter
        public void setLegacyFormatting(boolean useLegacyFormatting) {
            this.useLegacyFormatting = useLegacyFormatting;
        }

        @Override
        public boolean configure(final StaplerRequest req, final JSONObject json)
                throws FormException {
            useLegacyFormatting = false;
            parsingRulesGlobal = new ArrayList();
            req.bindJSON(this, json);
            save();
            return true;
        }

        public ListBoxModel doFillParsingRulesItems() {
            ListBoxModel items = new ListBoxModel();
            for (ParserRule rule : parsingRulesGlobal) {
                items.add(rule.getName(), rule.getRule());
                System.out.printf("%s\n", rule);
            }
            return items;
        }
    }

    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.NONE;
    }

    /*
     * This is read by the config.jelly : ${instance.parserRuleChoices} and
     * displays the available choices of parsing rules which were configured in
     * the global configurations
     */
    public List<ParserRule> getParserRuleChoices() {
        // Get the descriptor which holds the global configurations and extract
        // the available parsing rules from there
        return ((DescriptorImpl) this.getDescriptor()).getParsingRulesGlobal();
    }

    @Override
    public Action getProjectAction(AbstractProject<?, ?> project) {

        if (showGraphs) {
            return new LogParserProjectAction(project);

        } else {
            return null;
        }
    }

}
