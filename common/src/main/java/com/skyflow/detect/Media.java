package com.skyflow.detect;

/**
 * Media-type specific processing options. File inputs only.
 */
public final class Media {
    private final Audio audio;
    private final Document document;
    private final Image image;

    private Media(MediaBuilder builder) {
        this.audio = builder.audio;
        this.document = builder.document;
        this.image = builder.image;
    }

    /**
     * @return a new builder
     */
    public static MediaBuilder builder() {
        return new MediaBuilder();
    }

    /**
     * @return Audio options.
     */
    public Audio getAudio() {
        return audio;
    }

    /**
     * @return Document options.
     */
    public Document getDocument() {
        return document;
    }

    /**
     * @return Image options.
     */
    public Image getImage() {
        return image;
    }

    @Override
    public String toString() {
        return "Media{" +
                "audio=" + audio +
                ", document=" + document +
                ", image=" + image +
                '}';
    }

    public static final class MediaBuilder {
        private Audio audio;
        private Document document;
        private Image image;

        private MediaBuilder() {
        }

        /**
         * @param audio Audio options.
         * @return this builder
         */
        public MediaBuilder audio(Audio audio) {
            this.audio = audio;
            return this;
        }

        /**
         * @param document Document options.
         * @return this builder
         */
        public MediaBuilder document(Document document) {
            this.document = document;
            return this;
        }

        /**
         * @param image Image options.
         * @return this builder
         */
        public MediaBuilder image(Image image) {
            this.image = image;
            return this;
        }

        public Media build() {
            return new Media(this);
        }
    }
}
