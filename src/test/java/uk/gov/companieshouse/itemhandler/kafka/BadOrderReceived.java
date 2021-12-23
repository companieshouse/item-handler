package uk.gov.companieshouse.itemhandler.kafka;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import org.apache.avro.AvroRuntimeException;
import org.apache.avro.Schema;
import org.apache.avro.data.RecordBuilder;
import org.apache.avro.io.DatumReader;
import org.apache.avro.io.DatumWriter;
import org.apache.avro.specific.SpecificData;
import org.apache.avro.specific.SpecificDatumReader;
import org.apache.avro.specific.SpecificDatumWriter;
import org.apache.avro.specific.SpecificRecord;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.avro.specific.SpecificRecordBuilderBase;

public class BadOrderReceived extends SpecificRecordBase implements SpecificRecord {
    private static final long serialVersionUID = 4027526442472499302L;
    public static final Schema SCHEMA$ = (new Schema.Parser()).parse("{\"type\":\"record\",\"name\":\"BadOrderReceived\",\"namespace\":\"uk.gov.companieshouse.itemhandler.kafka\",\"fields\":[{\"name\":\"attempt\",\"type\":\"int\"},{\"name\":\"order_uri2\",\"type\":{\"type\":\"string\",\"avro.java.string\":\"String\"}},{\"name\":\"attempt2\",\"type\":\"int\"}]}");
    /** @deprecated */
    @Deprecated
    public String order_uri2;
    /** @deprecated */
    @Deprecated
    public int attempt;
    /** @deprecated */
    @Deprecated
    public int attempt2;
    private static final DatumWriter WRITER$;
    private static final DatumReader READER$;

    public static Schema getClassSchema() {
        return SCHEMA$;
    }

    public BadOrderReceived() {
    }

    public BadOrderReceived(String order_uri, Integer attempt, Integer attempt2) {
        this.order_uri2 = order_uri;
        this.attempt = attempt;
        this.attempt2 = attempt2;
    }

    public Schema getSchema() {
        return SCHEMA$;
    }

    public Object get(int field$) {
        switch(field$) {
            case 1:
                return this.order_uri2;
            case 0:
                return this.attempt;
            case 2:
                return this.attempt2;
            default:
                throw new AvroRuntimeException("Bad index");
        }
    }

    public void put(int field$, Object value$) {
        switch(field$) {
            case 1:
                this.order_uri2 = (String)value$;
                break;
            case 0:
                this.attempt = (Integer)value$;
                break;
            case 2:
                this.attempt2 = (Integer)value$;
                break;
            default:
                throw new AvroRuntimeException("Bad index");
        }

    }

    public String getOrderUri2() {
        return this.order_uri2;
    }

    public void setOrderUri2(String value) {
        this.order_uri2 = value;
    }

    public Integer getAttempt() {
        return this.attempt;
    }

    public void setAttempt(Integer value) {
        this.attempt = value;
    }

    public Integer getAttempt2() {
        return this.attempt2;
    }

    public void setAttempt2(Integer value) {
        this.attempt2 = value;
    }

    public static uk.gov.companieshouse.itemhandler.kafka.BadOrderReceived.Builder newBuilder() {
        return new uk.gov.companieshouse.itemhandler.kafka.BadOrderReceived.Builder();
    }

    public static uk.gov.companieshouse.itemhandler.kafka.BadOrderReceived.Builder newBuilder(uk.gov.companieshouse.itemhandler.kafka.BadOrderReceived.Builder other) {
        return new uk.gov.companieshouse.itemhandler.kafka.BadOrderReceived.Builder(other);
    }

    public static uk.gov.companieshouse.itemhandler.kafka.BadOrderReceived.Builder newBuilder(uk.gov.companieshouse.itemhandler.kafka.BadOrderReceived other) {
        return new uk.gov.companieshouse.itemhandler.kafka.BadOrderReceived.Builder(other);
    }

    public void writeExternal(ObjectOutput out) throws IOException {
        WRITER$.write(this, SpecificData.getEncoder(out));
    }

    public void readExternal(ObjectInput in) throws IOException {
        READER$.read(this, SpecificData.getDecoder(in));
    }

    static {
        WRITER$ = new SpecificDatumWriter(SCHEMA$);
        READER$ = new SpecificDatumReader(SCHEMA$);
    }

    public static class Builder extends SpecificRecordBuilderBase<uk.gov.companieshouse.itemhandler.kafka.BadOrderReceived>
            implements
            RecordBuilder<uk.gov.companieshouse.itemhandler.kafka.BadOrderReceived> {
        private String order_uri;
        private int attempt;
        private int attempt2;

        private Builder() {
            super(uk.gov.companieshouse.itemhandler.kafka.BadOrderReceived.SCHEMA$);
        }

        private Builder(uk.gov.companieshouse.itemhandler.kafka.BadOrderReceived.Builder other) {
            super(other);
            if (isValidValue(this.fields()[0], other.order_uri)) {
                this.order_uri = (String)this.data().deepCopy(this.fields()[0].schema(), other.order_uri);
                this.fieldSetFlags()[0] = true;
            }

            if (isValidValue(this.fields()[1], other.attempt)) {
                this.attempt = (Integer)this.data().deepCopy(this.fields()[1].schema(), other.attempt);
                this.fieldSetFlags()[1] = true;
            }

            if (isValidValue(this.fields()[2], other.attempt2)) {
                this.attempt2 = (Integer)this.data().deepCopy(this.fields()[2].schema(), other.attempt2);
                this.fieldSetFlags()[2] = true;
            }
        }

        private Builder(uk.gov.companieshouse.itemhandler.kafka.BadOrderReceived other) {
            super(uk.gov.companieshouse.itemhandler.kafka.BadOrderReceived.SCHEMA$);
            if (isValidValue(this.fields()[0], other.order_uri2)) {
                this.order_uri = (String)this.data().deepCopy(this.fields()[0].schema(), other.order_uri2);
                this.fieldSetFlags()[0] = true;
            }

            if (isValidValue(this.fields()[1], other.attempt)) {
                this.attempt = (Integer)this.data().deepCopy(this.fields()[1].schema(), other.attempt);
                this.fieldSetFlags()[1] = true;
            }

            if (isValidValue(this.fields()[2], other.attempt2)) {
                this.attempt2 = (Integer)this.data().deepCopy(this.fields()[1].schema(), other.attempt2);
                this.fieldSetFlags()[2] = true;
            }
        }

        public String getOrderUri() {
            return this.order_uri;
        }

        public uk.gov.companieshouse.itemhandler.kafka.BadOrderReceived.Builder setOrderUri(String value) {
            this.validate(this.fields()[0], value);
            this.order_uri = value;
            this.fieldSetFlags()[0] = true;
            return this;
        }

        public boolean hasOrderUri() {
            return this.fieldSetFlags()[0];
        }

        public uk.gov.companieshouse.itemhandler.kafka.BadOrderReceived.Builder clearOrderUri() {
            this.order_uri = null;
            this.fieldSetFlags()[0] = false;
            return this;
        }

        public Integer getAttempt() {
            return this.attempt;
        }

        public Integer getAttempt2() {
            return this.attempt2;
        }

        public uk.gov.companieshouse.itemhandler.kafka.BadOrderReceived.Builder setAttempt(int value) {
            this.validate(this.fields()[1], value);
            this.attempt = value;
            this.fieldSetFlags()[1] = true;
            return this;
        }

        public uk.gov.companieshouse.itemhandler.kafka.BadOrderReceived.Builder setAttempt2(int value) {
            this.validate(this.fields()[2], value);
            this.attempt2 = value;
            this.fieldSetFlags()[2] = true;
            return this;
        }

        public boolean hasAttempt() {
            return this.fieldSetFlags()[1];
        }

        public boolean hasAttempt2() {
            return this.fieldSetFlags()[2];
        }

        public uk.gov.companieshouse.itemhandler.kafka.BadOrderReceived.Builder clearAttempt() {
            this.fieldSetFlags()[1] = false;
            return this;
        }

        public uk.gov.companieshouse.itemhandler.kafka.BadOrderReceived.Builder clearAttempt2() {
            this.fieldSetFlags()[2] = false;
            return this;
        }

        public uk.gov.companieshouse.itemhandler.kafka.BadOrderReceived build() {
            try {
                uk.gov.companieshouse.itemhandler.kafka.BadOrderReceived record = new uk.gov.companieshouse.itemhandler.kafka.BadOrderReceived();
                record.order_uri2 = this.fieldSetFlags()[0] ? this.order_uri : (String)this.defaultValue(this.fields()[0]);
                record.attempt = this.fieldSetFlags()[1] ? this.attempt : (Integer)this.defaultValue(this.fields()[1]);
                record.attempt2 = this.fieldSetFlags()[1] ? this.attempt2 : (Integer)this.defaultValue(this.fields()[2]);
                return record;
            } catch (Exception var2) {
                throw new AvroRuntimeException(var2);
            }
        }
    }
}

