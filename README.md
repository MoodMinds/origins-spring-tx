# Interception of the [Origins](https://github.com/MoodMinds/origins)' `Emittable` and `Traversable` when marked with [Spring](https://spring.io)'s @Transactional

Transactional interceptor registration of the [Origins](https://github.com/MoodMinds/origins)' `Emittable` and
`Traversable` in Spring's `BeanFactoryTransactionAttributeSourceAdvisor` in Servlet non-reactive context.

## Usage

Include the provided `TraverseSupportTransactionAdvisory` in your Spring config and be able to use `Emittable` and `Traversable`
as return value in methods marked with `@Transactional` annotation:

```java
import org.moodminds.lang.Emittable;
import org.moodminds.lang.Traversable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static org.moodminds.lang.Emittable.emittable;
import static org.moodminds.lang.Traversable.traversable;

@Service
public class SampleService {

    @Transactional
    public Emittable<String, Exception> emission() {
        return emittable(traversable());
    }

    @Transactional
    public Traversable<String, Exception> traverse() {
        return traversable();
    }
}
```

The returning `Emittable` or `Traversable` will be intercepted and wrapped with its transactional equivalent.

## Maven configuration

Artifacts can be found on [Maven Central](https://search.maven.org/) after publication.

```xml
<dependency>
    <groupId>org.moodminds</groupId>
    <artifactId>origins-spring-tx</artifactId>
    <version>${version}</version>
</dependency>
```

## Building from Source

You may need to build from source to use **Origins Spring Transactions** (until it is in Maven Central) with Maven and JDK 9 at least.

## License
This project is going to be released under version 2.0 of the [Apache License][l].

[l]: https://www.apache.org/licenses/LICENSE-2.0