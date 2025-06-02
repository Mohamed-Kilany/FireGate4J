package api;

import io.cucumber.java.After;
import io.cucumber.java.Before;

public class Hooks {
    private final Context context;

    public Hooks(Context context) {
        this.context = context;
    }

    @Before
    public void before() {
        context.set("RestClient", new AsyncRestClient());
        context.set("SchemaValidator", new SchemaValidator());
        context.set("RegexGenerator", new RegexGenerator());
        context.set("PathExtractor", new PathExtractor());
        context.set("TypeConverter", new TypeConverter());
    }

    @After
    public void after() {
        context.reset();
    }
}
