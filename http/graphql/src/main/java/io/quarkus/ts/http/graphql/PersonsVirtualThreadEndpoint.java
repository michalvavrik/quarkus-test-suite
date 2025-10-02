package io.quarkus.ts.http.graphql;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import org.eclipse.microprofile.graphql.Description;
import org.eclipse.microprofile.graphql.GraphQLApi;
import org.eclipse.microprofile.graphql.Mutation;
import org.eclipse.microprofile.graphql.Name;
import org.eclipse.microprofile.graphql.Query;

import io.smallrye.common.annotation.RunOnVirtualThread;

@GraphQLApi
public class PersonsVirtualThreadEndpoint extends PersonsEndpointBase {

    @RunOnVirtualThread
    @Query("philosophers_vt")
    @Description("Get a couple of Greek philosophers")
    public List<Person> getPhilosophers() {
        return philosophers;
    }

    @RunOnVirtualThread
    @Query("friend_vt")
    public Person getPhilosopher(@Name("name") String name) {
        for (Person philosopher : philosophers) {
            if (philosopher.getName().equals(name)) {
                return philosopher.getFriend();
            }
        }
        throw new NoSuchElementException(name);
    }

    @RunOnVirtualThread
    @Mutation("create_vt")
    public Person createPhilosopher(@Name("name") String name) {
        Person philosopher = new Person(name);
        philosophers.add(philosopher);
        return philosopher;
    }

    @RunOnVirtualThread
    @Query("map_vt")
    public Map<PhilosophyEra, Person> getPhilosophersMap() {
        return philosophersMap;
    }

    @RunOnVirtualThread
    @Query("error_vt")
    public String throwError() throws PhilosophyException {
        throw new PhilosophyException();
    }
}
