package com.mriss.dsh.data.models;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;

import org.apache.commons.io.IOUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring {@link Configuration} providing {@link Document} beans for {@link DocumentTest}.
 * Loads only the minimal bean definitions needed – no MongoDB or full Boot autoconfiguration required.
 */
@Configuration
public class DocumentTestConfiguration {

    @Bean(name = "docTitleConstructor")
    public Document getDocumentWithTitle() throws NoSuchAlgorithmException, FileNotFoundException, IOException {
        Document d = new Document("Russia-Trump: FBI chief Wray defends agency");
        d.setOriginalFileContents(IOUtils.toByteArray(new FileInputStream(new File("target/test-classes/pdf/bbc-news-1.pdf"))));
        return d;
    }

    @Bean(name = "docTitleContentsConstructor")
    public Document getDocumentWithTitleAndContents() throws Exception {
        return new Document(IOUtils.toByteArray(new FileInputStream(new File("target/test-classes/pdf/bbc-news-1.pdf"))), "Russia-Trump: FBI chief Wray defends agency");
    }

    @Bean(name = "docTitleContentsFromStreamConstructor")
    public Document getDocumentWithTitleAndContentsFromStream() throws Exception {
        return new Document(new FileInputStream(new File("target/test-classes/pdf/bbc-news-1.pdf")), "Russia-Trump: FBI chief Wray defends agency");
    }

    @Bean(name = "anotherDocument")
    public Document getAnotherDocument() throws Exception {
        return new Document(new FileInputStream(new File("target/test-classes/pdf/edition.cnn.com-1.pdf")), "Emails show Trump Tower meeting follow-up");
    }
}

