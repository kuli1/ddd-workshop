package io.dddbyexamples.delivery.planning.delivery;

import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;
import io.dddbyexamples.delivery.planning.Amounts;
import io.dddbyexamples.delivery.planning.DeliveredAmountsChanged;
import io.dddbyexamples.delivery.planning.DeliveryEvents;
import io.dddbyexamples.delivery.planning.PlanningCompleted;
import io.dddbyexamples.delivery.planning.delivery.capacity.PayloadCapacityPolicy;
import io.dddbyexamples.delivery.planning.delivery.capacity.PayloadCapacityPolicyVer1;
import io.dddbyexamples.delivery.planning.plan.PlanCompleteness;
import io.dddbyexamples.delivery.planning.plan.ProductAmount;
import org.assertj.core.api.Assertions;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class DeliveryModificationsSteps {

    // dependencies
    private PayloadCapacityPolicy payloadCapacityPolicy = new PayloadCapacityPolicyVer1();

    // state
    private LocalDate date = LocalDate.now();
    private Map<String, StorageUnit> storageUnits = new HashMap<>();

    // consequences
    private Amounts exceeded = Amounts.empty();
    private DeliveryEvents events = createEventsListener();
    private DeliveredAmountsChanged planChanged;

    // objects under test
    private DeliveryFactory factory = new DeliveryFactory(payloadCapacityPolicy, events);
    private Map<Object, Delivery> deliveries = new HashMap<>();
    private PlanCompleteness planCompleteness;

    @Given("^(\\d+) pieces of product \"([^\"]*)\" are stored on \"([^\"]*)\"$")
    public void piecesOfProductAreStoredOn(int amount, String refNo, String storageUnitType) throws Throwable {
        storageUnits.put(refNo, new StorageUnit(storageUnitType, amount));
    }

    @Given("^customers demands for tomorrow:$")
    public void customersDemandsForTomorrow(List<ProductAmount> demands) throws Throwable {
        Amounts demandsForDate = ProductAmount.createAmounts(demands);
        this.planCompleteness = new PlanCompleteness(
                date, Amounts.empty(), demandsForDate, Amounts.empty()
        );
    }

    @Given("^delivery \"([^\"]*)\" scheduled at \"([^\"]*)\" with \"([^\"]*)\" of capacity (\\d+):$")
    public void DeliveryWithAliasWasScheduled(String alias,
                                              String time, String transportType,
                                              int transportCapacity,
                                              List<DeliveryPayload> payload) throws Throwable {
        scheduleNewDeliveryWithAlias(alias, time, transportType, transportCapacity, payload);
        planChanged = null;
    }

    @When("^new delivery is scheduled at \"([^\"]*)\" with \"([^\"]*)\" of capacity (\\d+):$")
    public void scheduleNewDelivery(String time, String transportType,
                                    int transportCapacity, List<DeliveryPayload> payload) throws Throwable {

        String alias = "transport at " + date + time;
        scheduleNewDeliveryWithAlias(alias, time, transportType, transportCapacity, payload);
    }

    @When("^new delivery \"([^\"]*)\" is scheduled at \"([^\"]*)\" with \"([^\"]*)\" of capacity (\\d+):$")
    public void scheduleNewDeliveryWithAlias(String alias,
                                             String time, String transportType,
                                             int transportCapacity,
                                             List<DeliveryPayload> payload) throws Throwable {
        Assertions.assertThat(deliveries).doesNotContainKeys(alias);
        createNewDelivery(alias);
        editDelivery(alias, time, transportType, transportCapacity, payload);
    }

    @When("^delivery \"([^\"]*)\" is edited: scheduled at \"([^\"]*)\" with \"([^\"]*)\" of capacity (\\d+):$")
    public void editDelivery(String alias,
                             String time, String transportType,
                             int transportCapacity,
                             List<DeliveryPayload> payload) throws Throwable {
        Delivery delivery = deliveries.get(alias);
        EditDelivery command = createCommand(delivery.getId(),
                LocalDateTime.of(date, LocalTime.parse(time)),
                transportType,
                transportCapacity,
                payload
        );
        try {
            delivery.editDelivery(command);
            exceeded = Amounts.empty();
        } catch (PayloadCapacityExceeded e) {
            exceeded = e.getExceeded();
        }
    }

    @When("^delivery \"([^\"]*)\" is cancelled$")
    public void deliveryIsCancelled(String id) throws Throwable {
        deliveries.get(id).cancelDelivery();
    }

    private Delivery createNewDelivery(String alias) {
        Delivery object = factory.createBlankDelivery();
        deliveries.put(alias, object);
        deliveries.put(object.getId(), object);
        return object;
    }

    private EditDelivery createCommand(UUID id, LocalDateTime time, String transportType, int transportCapacity, List<DeliveryPayload> payload) {
        return new EditDelivery(id,
                new Transport(time, new TransportType(transportType, transportCapacity)),
                DeliveryPayload.create(payload, storageUnits)
        );
    }

    @Then("^Transport capacity is not exceeded$")
    public void transportCapacityIsNotExceeded() throws Throwable {
        assertThat(exceeded.isEmpty()).isTrue();
    }

    @Then("^Transport capacity is exceeded$")
    public void transportCapacityInExceeded() throws Throwable {
        assertThat(exceeded.isEmpty()).isFalse();
    }

    @Then("^customers demands are fulfilled$")
    public void customersDemandsAreFulfilled() throws Throwable {
        assertThat(isDemandsFulfilled()).isTrue();
    }

    @Then("^customers demands are not fulfilled$")
    public void customersDemandsAreNotFulfilled() throws Throwable {
        assertThat(isDemandsFulfilled()).isFalse();
    }

    @Then("^plan was changed$")
    public void newDeliveryWasScheduled() throws Throwable {
        assertThat(planChanged).isNotNull();
    }

    @Then("^plan was NOT changed$")
    public void newDeliveryWasNotScheduled() throws Throwable {
        assertThat(planChanged).isNull();
    }

    private boolean isDemandsFulfilled() {
        return planCompleteness.getDiff()
                .allMatch((refNo, amount) -> amount >= 0L);
    }

    private DeliveryEvents createEventsListener() {
        return new DeliveryEvents() {
            @Override
            public void emit(DeliveredAmountsChanged event) {
                planChanged = event;
                planCompleteness.apply(event);
            }

            @Override
            public void emit(PlanningCompleted event) {
            }
        };
    }

}
