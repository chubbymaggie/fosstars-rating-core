package com.sap.sgs.phosphor.fosstars.data.github;

import static com.sap.sgs.phosphor.fosstars.model.feature.oss.OssFeatures.NUMBER_OF_CONTRIBUTORS_LAST_THREE_MONTHS;

import com.sap.sgs.phosphor.fosstars.model.Feature;
import com.sap.sgs.phosphor.fosstars.model.Value;
import com.sap.sgs.phosphor.fosstars.tool.github.GitHubProject;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

/**
 * This data provider counts how many people contributes to a project last 3 months. A contributor
 * is an author of a commit or a person who committed the commit. The provider iterates over all
 * commits which have been made for lat 3 months, and collect unique login names of contributors.
 */
public class NumberOfContributors extends CachedSingleFeatureGitHubDataProvider {

  /**
   * Period of time to be checked.
   */
  private static final Duration THREE_MONTHS = Duration.ofDays(90);

  /**
   * Initializes a data provider.
   *
   * @param fetcher An interface to GitHub.
   */
  public NumberOfContributors(GitHubDataFetcher fetcher) {
    super(fetcher);
  }

  @Override
  protected Feature supportedFeature() {
    return NUMBER_OF_CONTRIBUTORS_LAST_THREE_MONTHS;
  }

  @Override
  protected Value fetchValueFor(GitHubProject project) throws IOException {
    logger.info("Counting how many people contributed to the project in the last three months ...");

    Date date = Date.from(Instant.now().minus(THREE_MONTHS));
    Set<String> contributors = new HashSet<>();
    for (Commit commit : fetcher.localRepositoryFor(project).commitsAfter(date)) {
      contributors.add(commit.authorName());
      contributors.add(commit.committerName());
    }

    return NUMBER_OF_CONTRIBUTORS_LAST_THREE_MONTHS.value(contributors.size());
  }
}
