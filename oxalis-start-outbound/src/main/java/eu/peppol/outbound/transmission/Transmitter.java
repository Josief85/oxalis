package eu.peppol.outbound.transmission;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import eu.peppol.security.CommonName;
import eu.peppol.security.KeystoreManager;
import eu.peppol.start.identifier.AccessPointIdentifier;
import eu.peppol.start.identifier.ChannelId;
import eu.peppol.start.identifier.PeppolMessageHeader;
import eu.peppol.statistics.RawStatistics;
import eu.peppol.statistics.RawStatisticsRepository;

import java.util.Date;

/**
 * Executes transmission requests by sending the payload to the requested destination.
 * <p/>
 * <ol>
 * <li>Logs the fact that we are about to send</li>
 * <li>Logs the outcome of the transmission</li>
 * </ol>
 *
 * @author steinar
 *         Date: 04.11.13
 *         Time: 17:26
 */
public class Transmitter {

    private final MessageSenderFactory messageSenderFactory;
    private final RawStatisticsRepository rawStatisticsRepository;
    private final CommonName ourCommonName;
    private AccessPointIdentifier ourAccessPointIdentifier;


    @Inject
    public Transmitter(MessageSenderFactory messageSenderFactory, RawStatisticsRepository rawStatisticsRepository, @Named("OurCommonName")CommonName ourCommonName) {
        this.messageSenderFactory = messageSenderFactory;
        this.rawStatisticsRepository = rawStatisticsRepository;
        this.ourCommonName = ourCommonName;
        if (ourCommonName == null) {
            throw new IllegalArgumentException("Must supply the Common Name (CN) for our access point");
        }
        ourAccessPointIdentifier = new AccessPointIdentifier(ourCommonName.toString());
    }


    public TransmissionResponse transmit(TransmissionRequest transmissionRequest) {

        MessageSender messageSender = messageSenderFactory.createMessageSender(transmissionRequest.getEndpointAddress().getBusDoxProtocol());

        TransmissionResponse transmissionResponse = messageSender.send(transmissionRequest);

        persistStatistics(transmissionRequest, transmissionResponse);

        return transmissionResponse;
    }

    void persistStatistics(TransmissionRequest messageHeader, TransmissionResponse transmissionResponse) {


        RawStatistics.RawStatisticsBuilder builder = new RawStatistics.RawStatisticsBuilder()
                .accessPointIdentifier(ourAccessPointIdentifier)   // Identifier predefined in Oxalis global config file
                .outbound()
                .documentType(messageHeader.getPeppolStandardBusinessHeader().getDocumentTypeIdentifier())
                .sender(messageHeader.getPeppolStandardBusinessHeader().getSenderId())
                .receiver(messageHeader.getPeppolStandardBusinessHeader().getRecipientId())
                .profile(messageHeader.getPeppolStandardBusinessHeader().getProfileTypeIdentifier())
                .date(new Date());  // Time stamp of reception

        // If we know the CN name of the destination AP, supply that as the channel id
        if (messageHeader.getEndpointAddress().getCommonName() != null) {
            String accessPointIdentifierValue = messageHeader.getEndpointAddress().getCommonName().toString();
            builder.channel(new ChannelId(accessPointIdentifierValue));
        }

        RawStatistics rawStatistics = builder.build();

        rawStatisticsRepository.persist(rawStatistics);
    }

}