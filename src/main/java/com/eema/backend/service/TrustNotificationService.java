package com.eema.backend.service;

import com.eema.backend.config.AppProperties;
import com.eema.backend.entity.*;
import com.eema.backend.entity.enums.TrustEventType;
import com.eema.backend.event.TrustNotificationEvent;
import com.eema.backend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.HtmlUtils;

@Service @RequiredArgsConstructor
public class TrustNotificationService {
    private final OwnerVerificationRepository verifications;
    private final VenueRepository venues;
    private final OwnerPayoutDestinationRepository payoutDestinations;
    private final UserRepository users;
    private final EmailService email;
    private final AppProperties properties;

    @Transactional(readOnly=true)
    public void send(TrustNotificationEvent event) {
        if (event.type().name().startsWith("OWNER_VERIFICATION_")) ownerVerification(event);
        else if (event.type().name().startsWith("PAYOUT_DESTINATION_")) payoutDestination(event);
        else venue(event);
    }
    private void payoutDestination(TrustNotificationEvent event) {
        OwnerPayoutDestination destination=payoutDestinations.findById(event.subjectId()).orElse(null);
        if(destination==null)return;
        User owner=users.findById(destination.getOwnerId()).orElse(null); if(owner==null)return;
        String title; String message;
        switch(event.type()) {
            case PAYOUT_DESTINATION_SUBMITTED -> { title="Payout destination submitted"; message="Eema received your payout destination for manual verification."; }
            case PAYOUT_DESTINATION_VERIFIED -> { title="Payout destination verified"; message="Your payout destination is verified and may be used for future eligible payouts."; }
            case PAYOUT_DESTINATION_REJECTED -> { title="Your payout destination needs attention"; message="Your payout destination was not verified. Reason: "+safe(event.ownerVisibleReason()); }
            default -> { return; }
        }
        send(owner,title,message,base()+"/owner/payout-destination");
    }
    private void ownerVerification(TrustNotificationEvent event) {
        OwnerVerification v=verifications.findById(event.subjectId()).orElse(null); if(v==null)return;
        String title; String message;
        if(event.type()==TrustEventType.OWNER_VERIFICATION_SUBMITTED){title="Owner verification submitted";message="Eema received your owner verification submission for manual review.";}
        else if(event.type()==TrustEventType.OWNER_VERIFICATION_APPROVED){title="Your Eema owner verification was approved";message="Your owner verification is approved. You may submit production-ready venues for review.";}
        else {title="Your Eema owner verification needs attention";message="Your owner verification was not approved. Reason: "+safe(event.ownerVisibleReason());}
        send(v.getOwner(),title,message,base()+"/owner/verification");
    }
    private void venue(TrustNotificationEvent event) {
        Venue v=venues.findById(event.subjectId()).orElse(null);if(v==null)return;
        String title;String message;
        switch(event.type()){
            case VENUE_SUBMITTED->{title="Venue submitted for review";message="Eema received “"+v.getName()+"” for moderation.";}
            case VENUE_APPROVED->{title="Your venue is live";message="“"+v.getName()+"” was approved and is now live.";}
            case VENUE_CHANGES_REQUESTED->{title="Changes requested for your venue";message="Please update “"+v.getName()+"”. Reason: "+safe(event.ownerVisibleReason());}
            case VENUE_SUSPENDED->{title="Your venue was suspended";message="“"+v.getName()+"” is no longer publicly bookable. Reason: "+safe(event.ownerVisibleReason());}
            case VENUE_REACTIVATED->{title="Your venue is live again";message="“"+v.getName()+"” was reactivated.";}
            default->{return;}
        }
        send(v.getOwner(),title,message,base()+"/owner/venues/"+v.getId());
    }
    private void send(User owner,String subject,String message,String url){
        String text=message+"\n\nManage: "+url;
        email.send(owner.getEmail(),subject,text,"<p>"+HtmlUtils.htmlEscape(message)+"</p><p><a href=\""+url+"\">Manage in Eema</a></p>");
    }
    private String base(){return properties.frontendBaseUrl().replaceAll("/+$","");}
    private String safe(String s){return s==null||s.isBlank()?"Contact Eema support for details.":s;}
}
