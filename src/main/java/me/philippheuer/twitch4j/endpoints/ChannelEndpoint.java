package me.philippheuer.twitch4j.endpoints;

import java.util.*;

import me.philippheuer.twitch4j.auth.model.twitch.TwitchCredential;
import me.philippheuer.twitch4j.events.Event;
import me.philippheuer.twitch4j.events.event.FollowEvent;
import org.springframework.util.Assert;

import lombok.*;
import me.philippheuer.twitch4j.TwitchClient;
import me.philippheuer.twitch4j.model.*;

@Getter
@Setter
public class ChannelEndpoint extends AbstractTwitchEndpoint {

	/**
	 * Channel ID
	 */
	private Long channelId;

	/**
	 * Commercial Lengths
	 */
	private final List<Long> validCommercialLengths = new ArrayList<Long>(Arrays.asList(30L, 60L, 90L, 120L, 150L, 180L));

	/**
	 * Event Timer
	 */
	Timer eventTriggerTimer = new Timer(true);

	/**
	 * Constructor
	 */
	public ChannelEndpoint(TwitchClient client, Long channelId) {
		super(client);

		// Validate Arguments
		Assert.notNull(channelId, "Please provide a Channel ID!");

		// Process Arguments
		setChannelId(channelId);

		// Channel exists?
		Assert.isTrue(getChannel().isPresent(), "Target Channel " + channelId + " does not exists!");
	}

	/**
	 * Endpoint: Get Channel
	 *  Gets a specified channel object.
	 * Requires Scope: none
	 */
	public Optional<Channel> getChannel() {
		// REST Request
		try {
			String requestUrl = String.format("%s/channels/%s", getTwitchClient().getTwitchEndpoint(), getChannelId());
			if(!restObjectCache.containsKey(requestUrl)) {
				Channel responseObject = getTwitchClient().getRestClient().getRestTemplate().getForObject(requestUrl, Channel.class);
				restObjectCache.put(requestUrl, responseObject);
			}

			Channel responseObject = (Channel)restObjectCache.get(requestUrl);

			// Add twitch oauth credentials to channel object, if the credential manager has them
			if(getTwitchClient().getCredentialManager().getTwitchCredentialsForChannel(responseObject).isPresent()) {
				responseObject.setTwitchCredential(getTwitchClient().getCredentialManager().getTwitchCredentialsForChannel(responseObject));
			}

			return Optional.ofNullable(responseObject);
		} catch (Exception ex) {
			return Optional.empty();
		}
	}

	/**
	 * Endpoint: Get Channel
	 *  Get Channel returns more data than Get Channel by ID because Get Channel is privileged.
	 * Requires Scope: none
	 */
	public Optional<Channel> getChannelPrivilegied() {
		TwitchCredential twitchCredential = getTwitchClient().getCredentialManager().getTwitchCredentialsForChannel(getChannel().get()).get();

		// REST Request
		try {
			String requestUrl = String.format("%s/channel", getTwitchClient().getTwitchEndpoint());
			if(!restObjectCache.containsKey(requestUrl)) {
				Channel responseObject = getTwitchClient().getRestClient().getRestTemplate().getForObject(requestUrl, Channel.class);
				restObjectCache.put(requestUrl, responseObject);
			}

			Channel responseObject = (Channel)restObjectCache.get(requestUrl);

			return Optional.ofNullable(responseObject);
		} catch (Exception ex) {
			return Optional.empty();
		}
	}

	/**
	 * Endpoint: Get Channel Editors
	 *  Gets a list of users who are editors for a specified channel.
	 * Requires Scope: channel_read
	 */
	public Optional<List<User>> getEditors() {
		// REST Request
		try {
			String requestUrl = String.format("%s/channels/%s/editors", getTwitchClient().getTwitchEndpoint(), getChannelId());
			UserList responseObject = getTwitchClient().getRestClient().getRestTemplate().getForObject(requestUrl, UserList.class);

			return Optional.ofNullable(responseObject.getUsers());
		} catch (Exception ex) {
			return Optional.empty();
		}
	}

	/**
	 * Endpoint: Get Channel Followers
	 *  Gets a list of users who follow a specified channel, sorted by the date when they started following the channel (newest first, unless specified otherwise).
	 * Requires Scope: none
	 */
	public Optional<List<Follow>> getFollowers() {
		// REST Request
		try {
			String requestUrl = String.format("%s/channels/%s/follows", getTwitchClient().getTwitchEndpoint(), getChannelId());
			FollowList responseObject = getTwitchClient().getRestClient().getRestTemplate().getForObject(requestUrl, FollowList.class);

			return Optional.ofNullable(responseObject.getFollows());
		} catch (Exception ex) {
			return Optional.empty();
		}
	}

	/**
	 * Endpoint: Get Channel Teams
	 *  Gets a list of teams to which a specified channel belongs.
	 * Requires Scope: none
	 */
	public Optional<List<Team>> getTeams() {
		// REST Request
		try {
			String requestUrl = String.format("%s/channels/%s/teams", getTwitchClient().getTwitchEndpoint(), getChannelId());
			TeamList responseObject = getTwitchClient().getRestClient().getRestTemplate().getForObject(requestUrl, TeamList.class);

			return Optional.ofNullable(responseObject.getTeams());
		} catch (Exception ex) {
			return Optional.empty();
		}
	}

	/**
	 * Endpoint: Get Channel Subscribers
	 *  Gets a list of users subscribed to a specified channel, sorted by the date when they subscribed.
	 *  This is not related to the user messages, subscriptions are visible immediately.
	 * Requires Scope: channel_subscriptions
	 */
	public Optional<List<Subscription>> getSubscriptions() {
		// REST Request
		try {
			String requestUrl = String.format("%s/channels/%s/subscriptions", getTwitchClient().getTwitchEndpoint(), getChannelId());
			SubscriptionList responseObject = getTwitchClient().getRestClient().getRestTemplate().getForObject(requestUrl, SubscriptionList.class);

			return Optional.ofNullable(responseObject.getSubscriptions());
		} catch (Exception ex) {
			return Optional.empty();
		}
	}

	/**
	 * Endpoint: Get Channel Videos
	 *  Gets a list of users subscribed to a specified channel, sorted by the date when they subscribed.
	 *  This is not related to the user messages, subscriptions are visible immediately.
	 * Requires Scope: none
	 */
	public Optional<List<Video>> getVideos() {
		// REST Request
		try {
			String requestUrl = String.format("%s/channels/%s/videos", getTwitchClient().getTwitchEndpoint(), getChannelId());
			VideoList responseObject = getTwitchClient().getRestClient().getRestTemplate().getForObject(requestUrl, VideoList.class);

			return Optional.ofNullable(responseObject.getVideos());
		} catch (Exception ex) {
			return Optional.empty();
		}
	}

	/**
	 * Endpoint: Check Channel Subscription by User
	 *  Checks if a specified channel has a specified user subscribed to it. Intended for use by channel owners.
	 *  Returns a subscription object which includes the user if that user is subscribed. Requires authentication for the channel.
	 * Requires Scope: channel_check_subscription
	 */
	public Boolean getSubscriptionByUser(User user) {
		// Validate Arguments
		Assert.notNull(user, "Please provide a User!");

		// Get Channel
		Channel channel = getChannel().get();

		// REST Request
		try {
			String requestUrl = String.format("%s/channels/%s/subscriptions/%d", getTwitchClient().getTwitchEndpoint(), getChannelId(), user.getId());
			Subscription responseObject = getTwitchClient().getRestClient().getRestTemplate().getForObject(requestUrl, Subscription.class);

			getLogger().debug(String.format("Found Subscription for Channel %s [%s] for User %s [%s].", channel.getDisplayName(), channel.getId(), responseObject.getUser().getDisplayName(), responseObject.getUser().getId()));

			return true;
		} catch (Exception ex) {
			return false;
		}
	}

	/**
	 * Endpoint: Start Channel Commercial
	 *  Starts a commercial (advertisement) on a specified channel. This is valid only for channels that are Twitch partners.
	 *  You cannot start a commercial more often than once every 8 minutes.
	 *  The length of the commercial (in seconds) is specified in the request body, with a required length parameter.
	 *  Valid values are 30, 60, 90, 120, 150, and 180.
	 * Requires Scope: channel_commercial
	 */
	public Boolean startCommercial(Long length) {
		// Validate Arguments
		Assert.isTrue(getValidCommercialLengths().contains(length), "Please provide a valid length! Valid: " + getValidCommercialLengths().toString());

		// @TODO: Implementation
		// and check response for success

		return false;
	}

	/**
	 * Endpoint: Reset Channel Stream Key [!Irreversible]
	 *  Deletes the stream key for a specified channel. Once it is deleted, the stream key is automatically reset.
	 *  A stream key (also known as authorization key) uniquely identifies a stream.
	 *  Each broadcast uses an RTMP URL that includes the stream key. Stream keys are assigned by Twitch.
	 *  You will need to update your stream key or you will be unable to stream again.
	 * Requires Scope: channel_stream
	 */
	public Boolean deleteStreamKey() {
		// Get Channel
		Channel channel = getChannel().get();

		// REST Request
		try {
			String requestUrl = String.format("%s/channels/%s/stream_key", getTwitchClient().getTwitchEndpoint(), getChannelId());
			getTwitchClient().getRestClient().getRestTemplate().delete(requestUrl);

			getLogger().warn(String.format("Deleted Stream Key for Channel %s [%s]!", channel.getDisplayName(), channel.getId()));

			return true;
		} catch (Exception ex) {
			return false;
		}
	}

	/**
	 * New Event Checker: Last Follow
	 */
	private Date lastFollow;

	/**
	 * Central Endpoint: Register Channel Event Listener
	 *  IRC: Subscriptions
	 *  PubSub: Bits
	 *
	 */

	public void setChannelEventListener(Object annotationListener) {
		// Check that the channel exists
		// TODO

		// Check Endpoint Status
		// - Check Rest API
		// - Check IRC
		{
			Map.Entry<Boolean, String> result = getTwitchClient().getIrcClient().checkEndpointStatus();
			if(!result.getKey()) {
				getLogger().warn("IRC Client not operating. You will not receive any irc events! [" + result.getValue() + "]");
				return;
			}
		}
		// - Check PubSub
		if(!getTwitchClient().getPubSub().checkEndpointStatus()) {
			// We can ignore this right now, because we will reconnect as soon as pubsub is back up.
			getLogger().warn("PubSub Client not operating. You will not recieve any pubsub events!");
		}

		// Register Listener Events
		getTwitchClient().getDispatcher().registerListener(annotationListener);

		// Get Channel Information
		Channel channel = getChannel().get();
		// - Listen: IRC
		getTwitchClient().getIrcClient().joinChannel(channel.getName());
		// - Listen: PubSub
		// NYI

		// Event Timer
		eventTriggerTimer.scheduleAtFixedRate(
			new TimerTask() {
				public void run() {
					// Followers
					List<Date> creationDates = new ArrayList<Date>();
					Optional<List<Follow>> followList = getFollowers();
					if(followList.isPresent()) {
						for(Follow follow : followList.get()) {
							// dispatch event for new follows only
							if(lastFollow != null && follow.getCreatedAt().after(lastFollow)) {
								Event dispatchEvent = new FollowEvent(channel, follow.getUser());
								getTwitchClient().getDispatcher().dispatch(dispatchEvent);
							}
							creationDates.add(follow.getCreatedAt());
						}
					} else {
						getLogger().warn("Couldn't get followers for the event dispatcher!");
					}

					// Get newest date from all follows
					Date lastFollowNew = creationDates.stream().max(Date::compareTo).get();
					if(lastFollow == null || lastFollowNew.after(lastFollow)) {
						lastFollow = lastFollowNew;
					}
				}
			}, 0, 5 * 1000);
	}

	/**
	 * Cancel Timer/Listeners
	 */
	public void cancel() {
		eventTriggerTimer.cancel();
	}
}
