package application;

import java.util.Optional;
import java.util.concurrent.ExecutionException;
import org.eclipse.ditto.base.model.common.HttpStatus;
import org.eclipse.ditto.base.model.json.JsonSchemaVersion;
import org.eclipse.ditto.client.DittoClient;
import org.eclipse.ditto.client.DittoClients;
import org.eclipse.ditto.client.configuration.BasicAuthenticationConfiguration;
import org.eclipse.ditto.client.configuration.MessagingConfiguration;
import org.eclipse.ditto.client.configuration.WebSocketMessagingConfiguration;
import org.eclipse.ditto.client.live.LiveThingHandle;
import org.eclipse.ditto.client.messaging.AuthenticationProvider;
import org.eclipse.ditto.client.messaging.AuthenticationProviders;
import org.eclipse.ditto.client.messaging.MessagingProvider;
import org.eclipse.ditto.client.messaging.MessagingProviders;
import org.eclipse.ditto.json.JsonFactory;
import org.eclipse.ditto.protocol.Adaptable;
import org.eclipse.ditto.protocol.JsonifiableAdaptable;
import org.eclipse.ditto.protocol.ProtocolFactory;
import org.eclipse.ditto.things.model.ThingId;
import com.neovisionaries.ws.client.WebSocket;

public class CarsClient {
    
    public static void main(String[] args) {
        new CarsClient();
    }
    
    private CarHttpRequests httpRequests;
    private AuthenticationProvider<WebSocket> authenticationProvider;
    private MessagingProvider messagingProvider;
    private DittoClient client;
    
    private void createAuthProvider() {
        authenticationProvider = AuthenticationProviders.basic((
                BasicAuthenticationConfiguration
                .newBuilder()
                .username("ditto")
                .password("ditto")
                .build()));
    }
    
    private void createMessageProvider() {
        MessagingConfiguration.Builder builder = WebSocketMessagingConfiguration.newBuilder()
                .endpoint("ws://localhost:8080/ws/2")
                .jsonSchemaVersion(JsonSchemaVersion.V_2)
                .reconnectEnabled(false);
        messagingProvider = MessagingProviders.webSocket(builder.build(), authenticationProvider);
    }
    
    public CarsClient() {
        createDittoClient();
        httpRequests = new CarHttpRequests();
        if(checkIfThingExists().getCode() == 404) {
        	createCarThing();
        	resetThing();
        }
        else {
        	resetThing();
        }
        //subscribeForNotification();
        subscribeForMessages();
        //supervisor = new MaintenanceSupervisor(this);
        
    }
    
    private void createDittoClient() {
        createAuthProvider();
        createMessageProvider();
        client = DittoClients.newInstance(messagingProvider)
                .connect()
                .toCompletableFuture()
                .join();
    }
    
    
    
    private void subscribeForMessages() {
        try {
            client.live().startConsumption().toCompletableFuture().get();
        } catch (Exception e) {
            e.printStackTrace();
        }
        ThingId thingId = ThingId.of("io.eclipseprojects.ditto", "car");
        final LiveThingHandle thingIdLive = client.live().forId(thingId);
        // Register for *all* messages of a *specific* thing and provide payload as String
        thingIdLive.registerForMessage("msg_maintenance", "car.maintenance", String.class, message -> {
            final Optional<String> payload = message.getPayload();
            //System.out.println(payload.get());
            if(payload.get().equals("engine-indicator")) {
                System.out.println(thingId.getNamespace() + thingId.getName() + " needs an engine maintenance.");
            }
            else {
                if(payload.get().equals("battery-indicator")) {
                    System.out.println(thingId.getNamespace() + thingId.getName() + " needs a battery maintenance.");
                }
            }
        });
    }
    
    //Crea il Thing Car
    private void createCarThing() {
        System.out.println("Creating Twin \"io.eclipseprojects.ditto:car\"");
        int returnCode = httpRequests.createThing();
        if(returnCode == 201) {
            System.out.println("Twin io.eclipse.projects.ditto:car was created succesfully!");
        }
        else {
            System.out.println("Something happened in the creation of the twin.");
        }
    }
    
    //Controlla se il Twin Car è già stato creato
    private HttpStatus checkIfThingExists() {
    	
    	JsonifiableAdaptable jsonifiableAdaptable = ProtocolFactory.jsonifiableAdaptableFromJson(
                JsonFactory.readFrom("{\n"
                        + "  \"topic\": \"io.eclipseprojects.ditto/car/things/twin/commands/retrieve\",\n"
                        + "  \"headers\": {\n"
                        + "    \"correlation-id\": \"<command-correlation-id>\"\n"
                        + "  },\n"
                        + "  \"path\": \"/\"\n"
                        + "}\n"
                        + "").asObject());
    	HttpStatus p = null;
    	
        try {
            Adaptable adapt = client.sendDittoProtocol(jsonifiableAdaptable).toCompletableFuture().get();
            p = adapt.getPayload().getHttpStatus().get();
            if(p.getCode() == 404) {
            	//System.out.println(adapt.getPayload().getValue().get());
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        return p;
    }
    
    //Resetta le caratteristiche del Twin Car ad inizio simulazione
    private void resetThing() {
        System.out.println("resetting");
        JsonifiableAdaptable jsonifiableAdaptable = ProtocolFactory.jsonifiableAdaptableFromJson(
                JsonFactory.readFrom("{\n"
                        + "  \"topic\": \"io.eclipseprojects.ditto/car/things/twin/commands/modify\",\n"
                        + "  \"headers\": {\n"
                        + "    \"correlation-id\": \"<command-correlation-id>\"\n"
                        + "  },\n"
                        + "  \"path\": \"/features\",\n"
                        + "  \"value\": {\n"
                        + "    \n"
                        + "   \"status\": {\n"
                        + "      \"definition\": [\n"
                        + "         \"https://raw.githubusercontent.com/aleshark87/WoTModels/main/status.jsonld\"\n"
                        + "      ],\n"
                        + "      \"properties\": {\n"
                        + "         \"engine\": false,\n"
                        + "         \"charge-level\": 100.0\n"
                        + "      }\n"
                        + "   },\n"
                        + "   \"indicator-light\": {\n"
                        + "      \"definition\": [\n"
                        + "         \"https://raw.githubusercontent.com/aleshark87/WoTModels/main/indicator-light.jsonld\"\n"
                        + "      ],\n"
                        + "      \"properties\": {\n"
                        + "         \"battery-indicator\": false,\n"
                        + "         \"engine-indicator\": false\n"
                        + "      }\n"
                        + "   },\n"
                        + "   \"wear-time\": {\n"
                        + "      \"definition\": [\n"
                        + "         \"https://raw.githubusercontent.com/aleshark87/WoTModels/main/WearTime.jsonld\"\n"
                        + "        ],\n"
                        + "      \"properties\": {\n"
                        + "         \"battery-wear\": 0,\n"
                        + "         \"engine-wear\": 0\n"
                        + "      }\n"
                        + "   }\n"
                        + "\n"
                        + "  }\n"
                        + "}").asObject());
        client.sendDittoProtocol(jsonifiableAdaptable).whenComplete((a, t) -> {
            if (a != null) {
                //System.out.println(a);
            }
            if (t != null) {
                //System.out.println("sendDittoProtocol: Received throwable as response" + t);
            }
        });
    }

}
