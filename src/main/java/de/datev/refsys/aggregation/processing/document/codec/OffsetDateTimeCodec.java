package de.datev.refsys.aggregation.processing.document.codec;

import org.bson.BsonReader;
import org.bson.BsonWriter;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

public class OffsetDateTimeCodec implements Codec<OffsetDateTime> {


    private static final String DATE_TIME = "dateTime";
    private static final String ZONE = "zone";

    @Override
    public OffsetDateTime decode(BsonReader reader, DecoderContext decoderContext) {
        reader.readStartDocument();
        final Instant dateTime = Instant.ofEpochMilli(reader.readDateTime(DATE_TIME));
        final ZoneOffset offset = ZoneOffset.of(reader.readString(ZONE));
        reader.readEndDocument();

        return OffsetDateTime.ofInstant(dateTime, offset);
    }

    @Override
    public void encode(BsonWriter writer, OffsetDateTime offsetDateTime, EncoderContext encoderContext) {
        writer.writeStartDocument();
        writer.writeDateTime(DATE_TIME, offsetDateTime.toInstant().toEpochMilli());
        writer.writeString(ZONE, offsetDateTime.getOffset().toString());
        writer.writeEndDocument();
    }

    @Override
    public Class<OffsetDateTime> getEncoderClass() {
        return OffsetDateTime.class;
    }

}
