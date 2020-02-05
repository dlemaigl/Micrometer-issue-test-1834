import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.composite.CompositeMeterRegistry;
import io.micrometer.core.instrument.config.MeterFilter;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.ArrayList;
import java.util.List;

public class MicrometerFilterTest {

    //countries
    public static final String SPAIN = "spain";
    public static final String JAPAN = "japan";

    // Regions
    public static final String EUROPE = "europe";
    public static final String ASIA = "asia";

    //metrics names
    public static final String COUNTER_ONE = "counter.one";

    //metrics tags keys
    public static final String COUNTRY_TAG_KEY = "country";
    public static final String REGION_TAG_KEY = "region";



    public static void main(String[] args) {


        CompositeMeterRegistry compReg = new CompositeMeterRegistry();

        // the same counter with a tag of country with spain and japan values
        compReg.counter(COUNTER_ONE, COUNTRY_TAG_KEY, SPAIN).increment();
        compReg.counter(COUNTER_ONE, COUNTRY_TAG_KEY, JAPAN).increment();

        //One registry per region is created (ASIA, EUROPE) and added to the composite registry
        //the metrics will be filtered by region
        final MeterRegistry asianRegistry = createCustomMeterRegistry(ASIA);
        compReg.add(asianRegistry);
        final MeterRegistry europeanRegistry = createCustomMeterRegistry(EUROPE);
        compReg.add(europeanRegistry);

        // we try to get the counters, in theory we should get only one per registry
        // but we don't get one
        final Counter counterAsian = asianRegistry.find(COUNTER_ONE).counter();
        final Counter counterEuropean = europeanRegistry.find(COUNTER_ONE).counter();

        // the counters are both null!
        if(counterAsian == null) {
            System.out.println("counterAsian is null!");
        } else{
            System.out.println("counterAsian exist!");
        }

        if(counterEuropean == null) {
            System.out.println("counterEuropean is null!");
        } else {
            System.out.println("counterEuropean exist!");
        }

        compReg.counter(COUNTER_ONE, COUNTRY_TAG_KEY, SPAIN).increment();
        compReg.counter(COUNTER_ONE, COUNTRY_TAG_KEY, JAPAN).increment();
    }

    private static MeterRegistry createCustomMeterRegistry(String region){
        SimpleMeterRegistry smr = new SimpleMeterRegistry();

        //first we get create a new tag region from the country
        smr.config().meterFilter(new MeterFilter() {
            @Override
            public Meter.Id map(Meter.Id id) {

                List<Tag> tags = new ArrayList<>();
                for (Tag tag : id.getTagsAsIterable()) {
                    if(COUNTRY_TAG_KEY.equals(tag.getKey())){
                        //we create a new tag (region) from the value of country tag
                        String region = getRegionFromCountry(tag.getValue());
                        tags.add(Tag.of(REGION_TAG_KEY,region));
                    } else {
                        tags.add(tag);
                    }
                }

                return id.withTags(tags);
            }
        });

        //then we only accept the metrics of a specific tag region
        //that was created in the MeterFilter.map before
        smr.config().meterFilter(MeterFilter.deny(
            // this id should have the mapped region tag, but
            //because of issue 1834 we get the originals tags instead
            id-> !region.equals(id.getTag(REGION_TAG_KEY))));

        return smr;
    }

    private static String getRegionFromCountry(String value) {

        if(SPAIN.equals(value)){
            return EUROPE;
        } else if (JAPAN.equals(value)){
            return ASIA;
        }

        return "unknown";
    }


}
