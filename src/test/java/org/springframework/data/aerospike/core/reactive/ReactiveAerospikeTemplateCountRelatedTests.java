package org.springframework.data.aerospike.core.reactive;

import org.junit.Test;
import org.springframework.data.aerospike.core.Person;
import org.springframework.data.aerospike.core.ReactiveAerospikeTemplate;
import org.springframework.data.aerospike.repository.query.Criteria;
import org.springframework.data.aerospike.repository.query.Query;
import org.springframework.data.repository.query.parser.Part;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for count related methods in {@link ReactiveAerospikeTemplate}.
 *
 * @author Igor Ermolenko
 */
public class ReactiveAerospikeTemplateCountRelatedTests extends BaseReactiveAerospikeTemplateTests {

    @Test
    public void count_shouldFindAllItemsByGivenCriteria() {
        reactiveTemplate.insert(new Person(nextId(), "vasili", 50)).block();
        reactiveTemplate.insert(new Person(nextId(), "vasili", 51)).block();
        reactiveTemplate.insert(new Person(nextId(), "vasili", 52)).block();
        reactiveTemplate.insert(new Person(nextId(), "petya", 52)).block();

        Long vasyaCount = reactiveTemplate.count(new Query(new Criteria().is("vasili", "firstName")), Person.class).block();
        assertThat(vasyaCount).isEqualTo(3);

        Long vasya51Count = reactiveTemplate.count(new Query(new Criteria().is("vasili", "firstName").and("age").is(51, "age")), Person.class).block();
        assertThat(vasya51Count).isEqualTo(1);

        Long petyaCount = reactiveTemplate.count(new Query(new Criteria().is("petya", "firstName")), Person.class).block();
        assertThat(petyaCount).isEqualTo(1);
    }

    @Test
    public void count_shouldFindAllItemsByGivenCriteriaAndRespectsIgnoreCase() {
        reactiveTemplate.insert(new Person(nextId(), "VaSili", 50)).block();
        reactiveTemplate.insert(new Person(nextId(), "vasILI", 51)).block();
        reactiveTemplate.insert(new Person(nextId(), "vasili", 52)).block();

        Query query1 = new Query(new Criteria().startingWith("vas", "firstName", Part.IgnoreCaseType.ALWAYS));
        assertThat(reactiveTemplate.count(query1, Person.class).block()).isEqualTo(3);

        Query query2 = new Query(new Criteria().startingWith("VaS", "firstName", Part.IgnoreCaseType.NEVER));
        assertThat(reactiveTemplate.count(query2, Person.class).block()).isEqualTo(1);
    }

    @Test
    public void count_shouldReturnZeroIfNoDocumentsByProvidedCriteriaIsFound() {
        Long count = reactiveTemplate.count(new Query(new Criteria().is("nastyushka", "firstName")), Person.class).block();

        assertThat(count).isZero();
    }

    @Test(expected = IllegalArgumentException.class)
    public void count_shouldRejectNullEntityClass() {
        reactiveTemplate.count(null, (Class<?>) null).block();
    }


}
