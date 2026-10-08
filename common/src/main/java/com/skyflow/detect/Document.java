package com.skyflow.detect;

/**
 * Document processing options.
 */
public final class Document {
    private final Pdf pdf;

    private Document(DocumentBuilder builder) {
        this.pdf = builder.pdf;
    }

    /**
     * @return a new builder
     */
    public static DocumentBuilder builder() {
        return new DocumentBuilder();
    }

    /**
     * @return PDF options.
     */
    public Pdf getPdf() {
        return pdf;
    }

    @Override
    public String toString() {
        return "Document{" +
                "pdf=" + pdf +
                '}';
    }

    public static final class DocumentBuilder {
        private Pdf pdf;

        private DocumentBuilder() {
        }

        /**
         * @param pdf PDF options.
         * @return this builder
         */
        public DocumentBuilder pdf(Pdf pdf) {
            this.pdf = pdf;
            return this;
        }

        public Document build() {
            return new Document(this);
        }
    }
}
