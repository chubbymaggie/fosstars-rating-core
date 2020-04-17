package com.sap.sgs.phosphor.fosstars.data.github;

import static com.sap.sgs.phosphor.fosstars.model.feature.oss.OssFeatures.USES_VERIFIED_SIGNED_COMMITS;

import com.sap.sgs.phosphor.fosstars.model.Value;
import com.sap.sgs.phosphor.fosstars.model.ValueSet;
import com.sap.sgs.phosphor.fosstars.model.value.BooleanValue;
import com.sap.sgs.phosphor.fosstars.model.value.UnknownValue;
import com.sap.sgs.phosphor.fosstars.tool.github.GitHubProject;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.kohsuke.github.GHCommit;
import org.kohsuke.github.GitHub;

/**
 * This data provider checks if a project uses verified signed commits. The check would be based on
 * a density ratio compared against a percentage threshold
 * ({@link #SIGNED_COMMIT_PERCENTAGE_THRESHOLD}).
 */
public class UsesSignedCommits extends AbstractGitHubDataProvider {

  /**
   * The date after which all the commits would be considered.
   */
  private static final Date ONE_YEAR_AGO = Date.from(Instant.now().minus(Duration.ofDays(365)));

  /**
   * The threshold in % of number of verified signed commits against total number of commits in a
   * year.
   */
  private static final double SIGNED_COMMIT_PERCENTAGE_THRESHOLD = 90;

  /**
   * Initializes a data provider.
   *
   * @param github An interface to the GitHub API.
   */
  public UsesSignedCommits(GitHub github) {
    super(github);
  }

  @Override
  protected UsesSignedCommits doUpdate(GitHubProject project, ValueSet values) {
    logger.info("Figuring out if the project uses verified signed commits ...");

    Optional<Value> something = cache.get(project, USES_VERIFIED_SIGNED_COMMITS);
    if (something.isPresent()) {
      values.update(something.get());
      return this;
    }

    Value<Boolean> signedCommits = usesSignedCommits(project);
    values.update(signedCommits);
    cache.put(project, signedCommits, tomorrow());

    return this;
  }

  /**
   * It gathers the list of commits within one year from the current date. Then it calculates
   * density ratio. The density ratio is the percentage calculation of number of verified signed
   * commits against the total number of commits in the year. If the density ratio in % crosses a
   * certain percentage threshold ({@link #SIGNED_COMMIT_PERCENTAGE_THRESHOLD}), then it says that
   * the project uses verified signed commits. Otherwise, it says that the project does not use
   * verified signed commits.
   *
   * @param project The project.
   * @return {@link BooleanValue} value of the feature.
   */
  private Value<Boolean> usesSignedCommits(GitHubProject project) {
    try {
      return new BooleanValue(USES_VERIFIED_SIGNED_COMMITS, askGithub(project));
    } catch (IOException e) {
      logger.warn("Couldn't fetch data from api.github.com!", e);
      return UnknownValue.of(USES_VERIFIED_SIGNED_COMMITS);
    }
  }

  /**
   * Check if a project uses signed verified commits.
   *
   * @param project The project.
   * @return boolean True if the project uses verified signed commits. Otherwise, False
   * @throws IOException caused during REST call to GitHub API.
   */  
  private boolean askGithub(GitHubProject project) throws IOException {
    List<GHCommit> yearCommits = gitHubDataFetcher().commitsAfter(ONE_YEAR_AGO, project, github);
    int counter = yearCommits.size();

    List<GHCommit> verifiedCommits =
        yearCommits.stream().filter(commit -> isCommitVerified(commit))
            .collect(Collectors.toList());
    int signedCounter = verifiedCommits.size();

    return counter > 0 && signedCounter > 0
        && (signedCounter * 100 / counter) >= SIGNED_COMMIT_PERCENTAGE_THRESHOLD;
  }

  /**
   * Check if the commit is signed.
   * 
   * @param commit of type {@link GHCommit}.
   * @return true if the commit is signed. Otherwise, false.
   */
  public static final boolean isCommitVerified(GHCommit commit) {
    try {
      return commit.getCommitShortInfo().getVerification().isVerified();
    } catch (IOException e) {
      throw new UncheckedIOException("Could not get the short info of commit", e);
    }
  }
}