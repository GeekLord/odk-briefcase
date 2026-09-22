/*
 * Copyright (C) 2018 Nafundi
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.opendatakit.briefcase;

import static java.lang.Boolean.TRUE;
import static org.opendatakit.briefcase.buildconfig.BuildConfig.SENTRY_DSN;
import static org.opendatakit.briefcase.buildconfig.BuildConfig.SENTRY_ENABLED;
import static org.opendatakit.briefcase.buildconfig.BuildConfig.VERSION;
import static org.opendatakit.briefcase.model.BriefcasePreferences.BRIEFCASE_TRACKING_CONSENT_PROPERTY;
import static org.opendatakit.briefcase.operations.ClearPreferences.CLEAR_PREFS;
import static org.opendatakit.briefcase.operations.Common.DEPRECATED_AGGREGATE_SERVER;
import static org.opendatakit.briefcase.operations.Common.MAX_HTTP_CONNECTIONS;
import static org.opendatakit.briefcase.operations.Common.SERVER_URL;
import static org.opendatakit.briefcase.operations.Export.EXPORT_FORM;
import static org.opendatakit.briefcase.operations.ImportFromODK.IMPORT_FROM_ODK;
import static org.opendatakit.briefcase.operations.PullFormFromAggregate.DEPRECATED_PULL_AGGREGATE;
import static org.opendatakit.briefcase.operations.PullFormFromAggregate.DEPRECATED_PULL_IN_PARALLEL;
import static org.opendatakit.briefcase.operations.PullFormFromAggregate.PULL_AGGREGATE;
import static org.opendatakit.briefcase.operations.PullFormFromAggregate.PULL_FORM_FROM_AGGREGATE;
import static org.opendatakit.briefcase.operations.PushFormToAggregate.PUSH_FORM_TO_AGGREGATE;
import static org.opendatakit.briefcase.ui.BriefcaseCLI.runLegacyCli;
import static org.opendatakit.briefcase.ui.MainBriefcaseWindow.launchGUI;
import static org.opendatakit.briefcase.util.Host.getOsName;

import io.sentry.Sentry;
import org.opendatakit.briefcase.model.BriefcasePreferences;
import org.opendatakit.briefcase.operations.PullFormFromCentral;
import org.opendatakit.briefcase.operations.PushFormToCentral;
import org.opendatakit.briefcase.reused.BriefcaseException;
import org.opendatakit.common.cli.Cli;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main launcher for Briefcase
 * <p>
 * It leverages the command-line {@link Cli} adapter to define operations and run
 * Briefcase with some command-line args
 */
public class Launcher {
  private static final Logger log = LoggerFactory.getLogger(Launcher.class);

  public static void main(String[] args) {
    BriefcasePreferences appPreferences = BriefcasePreferences.appScoped();
    if (!appPreferences.hasKey(BRIEFCASE_TRACKING_CONSENT_PROPERTY))
      appPreferences.put(BRIEFCASE_TRACKING_CONSENT_PROPERTY, TRUE.toString());

    if (SENTRY_ENABLED)
      initSentry(appPreferences);

    new Cli()
        .deprecate(DEPRECATED_PULL_AGGREGATE, PULL_AGGREGATE)
        .deprecate(DEPRECATED_PULL_IN_PARALLEL, MAX_HTTP_CONNECTIONS)
        .deprecate(DEPRECATED_AGGREGATE_SERVER, SERVER_URL)
        .register(PULL_FORM_FROM_AGGREGATE)
        .register(PullFormFromCentral.OPERATION)
        .register(PUSH_FORM_TO_AGGREGATE)
        .register(PushFormToCentral.OPERATION)
        .register(IMPORT_FROM_ODK)
        .register(EXPORT_FORM)
        .register(CLEAR_PREFS)
        .otherwise((cli, commandLine) -> {
          if (args.length == 0)
            launchGUI();
          else
            runLegacyCli(commandLine, cli::printHelp);
        })
        .onError(throwable -> {
          System.err.println(throwable instanceof BriefcaseException
              ? "Error: " + throwable.getMessage()
              : "Unexpected error in Briefcase. Please review briefcase.log for more information. For help, post to https://forum.getodk.org/c/support");
          log.error("Error", throwable);
          if (SENTRY_ENABLED) {
            Sentry.captureException(throwable);
            // Events are sent asynchronously; give them a chance to leave before exiting
            Sentry.flush(5000);
          }
          System.exit(1);
        })
        .run(args);
  }

  private static void initSentry(BriefcasePreferences appPreferences) {
    Sentry.init(options -> {
      options.setDsn(SENTRY_DSN);
      options.setRelease(VERSION);
      options.addInAppInclude("org.opendatakit");
      options.setTag("os", getOsName());
      options.setTag("jvm", System.getProperty("java.version"));
      // Prevent sending crash reports to Sentry if the user disables tracking
      options.setBeforeSend((event, hint) -> appPreferences
          .nullSafeGet(BRIEFCASE_TRACKING_CONSENT_PROPERTY)
          .map(Boolean::valueOf)
          .orElse(true) ? event : null);
    });
  }
}
