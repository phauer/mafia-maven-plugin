package nl.sijpesteijn.testing.fitnesse.plugins.managers;

import java.io.File;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import nl.sijpesteijn.testing.fitnesse.plugins.pluginconfigs.ReporterPluginConfig;
import nl.sijpesteijn.testing.fitnesse.plugins.utils.MafiaReportGenerator;
import nl.sijpesteijn.testing.fitnesse.plugins.utils.MafiaTestResult;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.reporting.MavenReportException;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;

import util.FileUtil;
import fitnesse.VelocityFactory;
import fitnesse.responders.run.ExecutionReport;
import fitnesse.responders.run.SuiteExecutionReport;
import fitnesse.responders.run.TestExecutionReport;
import fitnesse.responders.testHistory.PageHistory;
import fitnesse.responders.testHistory.TestHistory;
import fitnesse.responders.testHistory.TestResultRecord;
import fitnesse.wiki.PageType;

/**
 * Plugin manager responsible for collecting report results.
 * 
 */
public class ReporterPluginManager implements PluginManager {

	private final ReporterPluginConfig reporterPluginConfig;
	private VelocityContext velocityContext;
	File historyDirectory;

	/**
	 * 
	 * @param reporterPluginConfig
	 *            {@link nl.sijpesteijn.testing.fitnesse.plugins.pluginconfigs.ReporterPluginConfig}
	 */
	public ReporterPluginManager(final ReporterPluginConfig reporterPluginConfig) {
		this.reporterPluginConfig = reporterPluginConfig;
		this.historyDirectory = new File(reporterPluginConfig.getMafiaTestResultsDirectory());
	}

	/**
	 * Collect the reports
	 * 
	 * @throws MavenReportException
	 */
	@Override
	public void run() throws MojoFailureException, MojoExecutionException {
		try {
			final List<MafiaTestResult> mafiaTestResults = getMafiaTestResults();
			createMafiaOutputDirectory();
			final MafiaReportGenerator generator = new MafiaReportGenerator(reporterPluginConfig.getSink(),
					reporterPluginConfig.getResourceBundle(), reporterPluginConfig, mafiaTestResults);
			generator.generate();
		} catch (final MavenReportException e) {
			new MojoExecutionException("Could not generate mafia report: ", e);
		} catch (final Exception e) {
			new MojoExecutionException("Could not generate mafia report: ", e);
		}
	}

	private void createMafiaOutputDirectory() {
		new File(reporterPluginConfig.getFitnesseTestResultsDirectory()).mkdirs();
	}

	public List<MafiaTestResult> getMafiaTestResults() throws Exception {
		final List<MafiaTestResult> testResultRecords = new ArrayList<MafiaTestResult>();
		addSuiteResults(testResultRecords);
		addSuiteFilteredResults(testResultRecords);
		addTestResults(testResultRecords);
		return testResultRecords;
	}

	private void addSuiteResults(final List<MafiaTestResult> testResultRecords) throws Exception {
		final List<String> suites = reporterPluginConfig.getSuites();
		if (suites != null && !suites.isEmpty()) {
			for (final String suite : suites) {
				testResultRecords.add(getMafiaTestResult(suite, PageType.SUITE));
			}
		}
	}

	private void addSuiteFilteredResults(final List<MafiaTestResult> testResultRecords) throws Exception {
		final String suitePageName = reporterPluginConfig.getSuitePageName();
		if (suitePageName != null && !suitePageName.equals("")) {
			testResultRecords.add(getMafiaTestResult(suitePageName, PageType.SUITE));
		}
	}

	private void addTestResults(final List<MafiaTestResult> testResultRecords) throws Exception {
		final List<String> tests = reporterPluginConfig.getTests();
		if (tests != null && !tests.isEmpty()) {
			for (final String test : tests) {
				testResultRecords.add(getMafiaTestResult(test, PageType.TEST));
			}
		}
	}

	private MafiaTestResult getMafiaTestResult(final String pageName, final PageType pageType) throws Exception {
		final TestHistory history = new TestHistory();
		history.readPageHistoryDirectory(historyDirectory, pageName);
		final PageHistory pageHistory = history.getPageHistory(pageName);
		final Date latestDate = pageHistory.getLatestDate();
		final TestResultRecord testResultRecord = pageHistory.get(latestDate);
		velocityContext = new VelocityContext();

		final String content = FileUtil.getFileContent(testResultRecord.getFile());
		final ExecutionReport report = ExecutionReport.makeReport(content);
		String html = "";
		if (report instanceof TestExecutionReport) {
			report.setDate(latestDate);
			html = generateTestExecutionHTML((TestExecutionReport) report);
		} else if (report instanceof SuiteExecutionReport) {
			html = generateSuiteExecutionHTML((SuiteExecutionReport) report);
		}
		System.out.println(pageName + "*****************" + html);
		return new MafiaTestResult(pageType, pageName, testResultRecord, html);
	}

	private String generateSuiteExecutionHTML(final SuiteExecutionReport report) throws Exception {
		velocityContext.put("suiteExecutionReport", report);
		final Template template = VelocityFactory.getVelocityEngine().getTemplate("suiteExecutionReport.vm");
		return makeHTMLFromTemplate(template);
	}

	private String generateTestExecutionHTML(final TestExecutionReport report) throws Exception {
		velocityContext.put("testExecutionReport", report);
		final Template template = VelocityFactory.getVelocityEngine().getTemplate("testExecutionReport.vm");
		return makeHTMLFromTemplate(template);
	}

	private String makeHTMLFromTemplate(final Template template) throws Exception {
		final StringWriter writer = new StringWriter();
		template.merge(velocityContext, writer);
		return writer.toString();
	}
}
