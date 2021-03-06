package org.springframework.data.aerospike.core.reactive;

import com.aerospike.client.Key;
import com.aerospike.client.Record;
import com.aerospike.client.policy.Policy;
import com.aerospike.client.query.IndexType;
import org.junit.Test;
import org.springframework.data.aerospike.SampleClasses;
import org.springframework.data.aerospike.core.ReactiveAerospikeTemplate;
import org.springframework.data.aerospike.repository.query.Query;
import org.springframework.data.aerospike.sample.Person;
import org.springframework.data.domain.Sort;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.*;
import static org.springframework.data.aerospike.SampleClasses.EXPIRATION_ONE_MINUTE;

/**
 * Tests for find related methods in {@link ReactiveAerospikeTemplate}.
 *
 * @author Igor Ermolenko
 */
public class ReactiveAerospikeTemplateFindRelatedTests extends BaseReactiveAerospikeTemplateTests {
    @Test
    public void findById_shouldReturnValueForExistingKey() {
        Person person = new Person(id, "Dave", "Matthews");
        reactiveTemplate.save(person).block();
        Optional<Person> result = reactiveTemplate.findById(id, Person.class).block();
        assertTrue(result.isPresent());
        assertEquals("Matthews", result.get().getLastname());
        assertEquals("Dave", result.get().getFirstname());
    }

    @Test
    public void findById_shouldReturnNullForNonExistingKey() {
        Optional<Person> result = reactiveTemplate.findById("dave-is-absent", Person.class).block();
        assertFalse(result.isPresent());
    }

    @Test
    public void findById_shouldReturnNullForNonExistingKeyIfTouchOnReadSetToTrue() {
        Optional<SampleClasses.DocumentWithTouchOnRead> result =
                reactiveTemplate.findById("foo-is-absent", SampleClasses.DocumentWithTouchOnRead.class).block();
        assertFalse(result.isPresent());
    }

    @Test
    public void findById_shouldIncreaseVersionIfTouchOnReadSetToTrue() {
        SampleClasses.DocumentWithTouchOnRead document = new SampleClasses.DocumentWithTouchOnRead(id, 1);
        reactiveTemplate.save(document).block();

        Optional<SampleClasses.DocumentWithTouchOnRead> result = reactiveTemplate.findById(document.getId(), SampleClasses.DocumentWithTouchOnRead.class).block();
        assertThat(result.get().getVersion()).isEqualTo(document.getVersion() + 1);
    }

    @Test(expected = IllegalStateException.class)
    public void findById_shouldFailOnTouchOnReadWithExpirationProperty() {
        SampleClasses.DocumentWithTouchOnReadAndExpirationProperty document = new SampleClasses.DocumentWithTouchOnReadAndExpirationProperty(id, EXPIRATION_ONE_MINUTE);
        reactiveTemplate.insert(document).block();
        reactiveTemplate.findById(document.getId(), SampleClasses.DocumentWithTouchOnReadAndExpirationProperty.class);
    }

    @Test
    public void findAll_findsAllExistingDocuments() {
        List<Person> persons = IntStream.rangeClosed(1, 10)
                .mapToObj(age -> new Person(nextId(), "Dave", "Matthews", age))
                .collect(Collectors.toList());
        reactiveTemplate.insertAll(persons).blockLast();

        List<Person> result = reactiveTemplate.findAll(Person.class).collectList().block();
        assertThat(result).containsOnlyElementsOf(persons);
    }

    @Test
    public void findAll_findsNothing() throws Exception {
        List<Person> result = reactiveTemplate.findAll(Person.class).collectList().block();

        assertThat(result).isEmpty();
    }

    @Test
    public void findByIds_shouldReturnEmptyList() {
        Long userCount = reactiveTemplate.findByIds(Collections.emptyList(), Person.class).count().block();
        assertThat(userCount).isEqualTo(0);
    }

    @Test
    public void findByIds_shouldFindExisting() {
        Person customer1 = new Person(nextId(), "Dave", "Matthews");
        Person customer2 = new Person(nextId(), "James", "Bond");
        Person customer3 = new Person(nextId(), "Matt", "Groening");
        reactiveTemplate.insertAll(Arrays.asList(customer1, customer2, customer3)).blockLast();

        List<String> ids = Arrays.asList("unknown", customer1.getId(), customer2.getId());
        List<Person> actual = reactiveTemplate.findByIds(ids, Person.class).collectList().block();

        assertThat(actual).containsExactlyInAnyOrder(customer1, customer2);
    }

    @Test
    public void findInRange_shouldFindLimitedNumberOfDocuments() {
        List<Person> allUsers = IntStream.range(20, 27)
                .mapToObj(id -> new Person(nextId(), "Firstname", "Lastname")).collect(Collectors.toList());
        reactiveTemplate.insertAll(allUsers).blockLast();

        List<Person> actual = reactiveTemplate.findInRange(0, 5, Sort.unsorted(), Person.class).collectList().block();
        assertThat(actual)
                .hasSize(5)
                .containsAnyElementsOf(allUsers);
    }

    @Test
    public void findInRange_shouldFindLimitedNumberOfDocumentsAndSkip() {
        List<Person> allUsers = IntStream.range(20, 27)
                .mapToObj(id -> new Person(nextId(), "Firstname", "Lastname")).collect(Collectors.toList());
        reactiveTemplate.insertAll(allUsers).blockLast();

        List<Person> actual = reactiveTemplate.findInRange(0, 5, Sort.unsorted(), Person.class).collectList().block();

        assertThat(actual)
                .hasSize(5)
                .containsAnyElementsOf(allUsers);
    }

    @Test
    public void find_throwsExceptionForUnsortedQueryWithSpecifiedOffsetValue() {
        Query query = new Query((Sort) null);
        query.setOffset(1);

        assertThatThrownBy(() -> reactiveTemplate.find(query, Person.class).collectList().block())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Unsorted query must not have offset value. For retrieving paged results use sorted query.");
    }


    @Test
    public void find_shouldWorkWithFilterEqual() {
        createIndexIfNotExists(Person.class, "first_name_index", "firstname", IndexType.STRING);
        List<Person> allUsers = IntStream.rangeClosed(1, 10)
                .mapToObj(id -> new Person(nextId(), "Dave", "Matthews")).collect(Collectors.toList());
        reactiveTemplate.insertAll(allUsers).blockLast();

        Query query = createQueryForMethodWithArgs("findPersonByFirstname", "Dave");

        List<Person> actual = reactiveTemplate.find(query, Person.class).collectList().block();
        assertThat(actual)
                .hasSize(10)
                .containsExactlyInAnyOrderElementsOf(allUsers);
    }

    @Test
    public void find_shouldWorkWithFilterEqualOrderBy() {
        createIndexIfNotExists(Person.class, "age_index", "age", IndexType.NUMERIC);
        createIndexIfNotExists(Person.class, "last_name_index", "lastname", IndexType.STRING);

        List<Person> allUsers = IntStream.rangeClosed(1, 10)
                .mapToObj(id -> new Person(nextId(), "Dave" + id, "Matthews")).collect(Collectors.toList());
        Collections.shuffle(allUsers); // Shuffle user list
        reactiveTemplate.insertAll(allUsers).blockLast();
        allUsers.sort(Comparator.comparing(Person::getFirstname)); // Order user list by firstname ascending

        Query query = createQueryForMethodWithArgs("findByLastnameOrderByFirstnameAsc", "Matthews");

        List<Person> actual = reactiveTemplate.find(query, Person.class).collectList().block();
        assertThat(actual)
                .hasSize(10)
                .containsExactlyElementsOf(allUsers);
    }

    @Test
    public void find_shouldWorkWithFilterEqualOrderByDesc() {
        createIndexIfNotExists(Person.class, "age_index", "age", IndexType.NUMERIC);
        createIndexIfNotExists(Person.class, "last_name_index", "lastname", IndexType.STRING);

        List<Person> allUsers = IntStream.rangeClosed(1, 10)
                .mapToObj(id -> new Person(nextId(), "Dave" + id, "Matthews")).collect(Collectors.toList());
        Collections.shuffle(allUsers); // Shuffle user list
        reactiveTemplate.insertAll(allUsers).blockLast();
        allUsers.sort((o1, o2) -> o2.getFirstname().compareTo(o1.getFirstname())); // Order user list by firstname descending

        Query query = createQueryForMethodWithArgs("findByLastnameOrderByFirstnameDesc", "Matthews");

        List<Person> actual = reactiveTemplate.find(query, Person.class).collectList().block();
        assertThat(actual)
                .hasSize(10)
                .containsExactlyElementsOf(allUsers);
    }

    @Test
    public void find_shouldWorkWithFilterRange() {
        createIndexIfNotExists(Person.class, "age_index", "age", IndexType.NUMERIC);

        List<Person> allUsers = IntStream.rangeClosed(21, 30)
                .mapToObj(age -> new Person(nextId(), "Dave" + age, "Matthews", age)).collect(Collectors.toList());
        reactiveTemplate.insertAll(allUsers).blockLast();

        Query query = createQueryForMethodWithArgs("findCustomerByAgeBetween", 25, 30);

        List<Person> actual = reactiveTemplate.find(query, Person.class).collectList().block();

        assertThat(actual)
                .hasSize(6)
                .containsExactlyInAnyOrderElementsOf(allUsers.subList(4, 10));
    }

    @Test
    public void findById_shouldSetVersionEqualToNumberOfModifications() throws Exception {
        reactiveTemplate.insert(new SampleClasses.VersionedClass(id, "foobar")).block();
        reactiveTemplate.update(new SampleClasses.VersionedClass(id, "foobar1")).block();
        reactiveTemplate.update(new SampleClasses.VersionedClass(id, "foobar2")).block();

        Record raw = client.get(new Policy(), new Key(getNameSpace(), "versioned-set", id));
        assertThat(raw.generation).isEqualTo(3);
        Optional<SampleClasses.VersionedClass> actual = reactiveTemplate.findById(id, SampleClasses.VersionedClass.class).block();
        assertThat(actual.get().version).isEqualTo(3);
    }
}
