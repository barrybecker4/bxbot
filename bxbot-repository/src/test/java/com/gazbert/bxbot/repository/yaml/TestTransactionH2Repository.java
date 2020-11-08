package com.gazbert.bxbot.repository.yaml;

import static org.assertj.core.api.Java6Assertions.assertThat;

import com.gazbert.bxbot.datastore.yaml.ConfigurationManager;
import com.gazbert.bxbot.domain.transaction.TransactionEntry;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.easymock.PowerMock;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ConfigurationManager.class})
@PowerMockIgnore({
        "javax.crypto.*",
        "javax.management.*",
        "com.sun.org.apache.xerces.*",
        "javax.xml.parsers.*",
        "org.xml.sax.*",
        "org.w3c.dom.*"
})

public class TestTransactionH2Repository {

  @Test
  public void whenSaveCalledThenExpectRepositoryToSaveTransactionEntry() {

    PowerMock.replayAll();

    final TransactionEntry entry = new TransactionEntry("foo", "bar");

    final TransactionsYamlRepository transactionsYamlRepository =
            new TransactionsYamlRepository();
    final TransactionEntry retrievedEntry = transactionsYamlRepository.save(entry);

    assertThat(retrievedEntry)
            .isEqualTo(entry);

    PowerMock.verifyAll();
  }
}
