package net.bluemind.mailbox.api.rules;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import net.bluemind.core.api.BMApi;
import net.bluemind.core.api.GwtIncompatible;
import net.bluemind.mailbox.api.rules.actions.MailFilterRuleAction;
import net.bluemind.mailbox.api.rules.actions.MailFilterRuleActionAddHeaders;
import net.bluemind.mailbox.api.rules.actions.MailFilterRuleActionCategorize;
import net.bluemind.mailbox.api.rules.actions.MailFilterRuleActionCopy;
import net.bluemind.mailbox.api.rules.actions.MailFilterRuleActionDiscard;
import net.bluemind.mailbox.api.rules.actions.MailFilterRuleActionFollowUp;
import net.bluemind.mailbox.api.rules.actions.MailFilterRuleActionFollowUp.DueDate;
import net.bluemind.mailbox.api.rules.actions.MailFilterRuleActionFollowUp.FollowUpAction;
import net.bluemind.mailbox.api.rules.actions.MailFilterRuleActionMarkAsDeleted;
import net.bluemind.mailbox.api.rules.actions.MailFilterRuleActionMarkAsImportant;
import net.bluemind.mailbox.api.rules.actions.MailFilterRuleActionMarkAsRead;
import net.bluemind.mailbox.api.rules.actions.MailFilterRuleActionMove;
import net.bluemind.mailbox.api.rules.actions.MailFilterRuleActionName;
import net.bluemind.mailbox.api.rules.actions.MailFilterRuleActionPrioritize;
import net.bluemind.mailbox.api.rules.actions.MailFilterRuleActionRedirect;
import net.bluemind.mailbox.api.rules.actions.MailFilterRuleActionRemoveHeaders;
import net.bluemind.mailbox.api.rules.actions.MailFilterRuleActionReply;
import net.bluemind.mailbox.api.rules.actions.MailFilterRuleActionSetFlags;
import net.bluemind.mailbox.api.rules.actions.MailFilterRuleActionTransfer;
import net.bluemind.mailbox.api.rules.actions.MailFilterRuleActionUncategorize;
import net.bluemind.mailbox.api.rules.actions.MailFilterRuleActionUnfollow;
import net.bluemind.mailbox.api.rules.conditions.MailFilterRuleCondition;

@BMApi(version = "3")
public class MailFilterRule {

	@BMApi(version = "3")
	public enum Type {
		GENERIC, FORWARD, VACATION
	}

	@BMApi(version = "3")
	public enum Trigger {
		IN, OUT
	}

	public String client;
	public Type type = Type.GENERIC;
	public Trigger trigger = Trigger.IN;
	public boolean deferred = false;
	public boolean active = true;
	public String name;
	public List<MailFilterRuleCondition> conditions = new ArrayList<>();
	public List<MailFilterRuleAction> actions = new ArrayList<>();
	public boolean stop = true;

	public boolean hasAction() {
		return !actions.isEmpty();
	}

	public MailFilterRule removeAction(MailFilterRuleActionName name) {
		actions = actions.stream().filter(action -> action.name != name).collect(Collectors.toList());
		return this;
	}

	public Optional<MailFilterRuleActionSetFlags> setFlags() {
		return actions.stream() //
				.filter(action -> action.name.equals(MailFilterRuleActionName.SET_FLAGS)) //
				.map(action -> (MailFilterRuleActionSetFlags) action).findFirst();
	}

	public MailFilterRule addSetFlags(String... flags) {
		removeAction(MailFilterRuleActionName.SET_FLAGS);
		actions.add(new MailFilterRuleActionSetFlags(Arrays.asList(flags)));
		return this;
	}

	public Optional<MailFilterRuleActionMarkAsRead> markAsRead() {
		return actions.stream() //
				.filter(action -> action.name.equals(MailFilterRuleActionName.MARK_AS_READ)) //
				.map(action -> (MailFilterRuleActionMarkAsRead) action).findFirst();
	}

	public MailFilterRule addMarkAsRead() {
		removeAction(MailFilterRuleActionName.MARK_AS_READ);
		actions.add(new MailFilterRuleActionMarkAsRead());
		return this;
	}

	public Optional<MailFilterRuleActionMarkAsImportant> markAsImportant() {
		return actions.stream() //
				.filter(action -> action.name.equals(MailFilterRuleActionName.MARK_AS_IMPORTANT)) //
				.map(action -> (MailFilterRuleActionMarkAsImportant) action).findFirst();
	}

	public MailFilterRule addMarkAsImportant() {
		removeAction(MailFilterRuleActionName.MARK_AS_IMPORTANT);
		actions.add(new MailFilterRuleActionMarkAsImportant());
		return this;
	}

	public Optional<MailFilterRuleActionMarkAsDeleted> markAsDeleted() {
		return actions.stream() //
				.filter(action -> action.name.equals(MailFilterRuleActionName.MARK_AS_DELETED)) //
				.map(action -> (MailFilterRuleActionMarkAsDeleted) action).findFirst();
	}

	public MailFilterRule addMarkAsDeleted() {
		removeAction(MailFilterRuleActionName.MARK_AS_DELETED);
		actions.add(new MailFilterRuleActionMarkAsDeleted());
		return this;
	}

	public Optional<MailFilterRuleActionAddHeaders> addHeaders() {
		return actions.stream() //
				.filter(action -> action.name.equals(MailFilterRuleActionName.ADD_HEADER)) //
				.map(action -> (MailFilterRuleActionAddHeaders) action).findFirst();
	}

	public MailFilterRule addAddHeader(String name, String value) {
		return addAddHeaders(Collections.singletonMap(name, value));
	}

	public MailFilterRule addAddHeaders(Map<String, String> headers) {
		removeAction(MailFilterRuleActionName.ADD_HEADER);
		actions.add(new MailFilterRuleActionAddHeaders(headers));
		return this;
	}

	public Optional<MailFilterRuleActionCategorize> categorize() {
		return actions.stream() //
				.filter(action -> action.name.equals(MailFilterRuleActionName.CATEGORIZE)) //
				.map(action -> (MailFilterRuleActionCategorize) action).findFirst();
	}

	public MailFilterRule addCategorize(List<String> categories) {
		removeAction(MailFilterRuleActionName.CATEGORIZE);
		actions.add(new MailFilterRuleActionCategorize(categories));
		return this;
	}

	public Optional<MailFilterRuleActionFollowUp> followUp() {
		return actions.stream() //
				.filter(action -> action.name.equals(MailFilterRuleActionName.FOLLOW_UP)) //
				.map(action -> (MailFilterRuleActionFollowUp) action).findFirst();
	}

	public MailFilterRule addFollowUp(FollowUpAction action, DueDate dueDate) {
		removeAction(MailFilterRuleActionName.CATEGORIZE);
		actions.add(new MailFilterRuleActionFollowUp(action, dueDate));
		return this;
	}

	public Optional<MailFilterRuleActionPrioritize> prioritize() {
		return actions.stream() //
				.filter(action -> action.name.equals(MailFilterRuleActionName.PRIORITIZE)) //
				.map(action -> (MailFilterRuleActionPrioritize) action).findFirst();
	}

	public MailFilterRule addPrioritize(int priority) {
		removeAction(MailFilterRuleActionName.PRIORITIZE);
		actions.add(new MailFilterRuleActionPrioritize(String.valueOf(priority)));
		return this;
	}

	public Optional<MailFilterRuleActionRemoveHeaders> removeHeaders() {
		return actions.stream() //
				.filter(action -> action.name.equals(MailFilterRuleActionName.REMOVE_HEADERS)) //
				.map(action -> (MailFilterRuleActionRemoveHeaders) action).findFirst();
	}

	public MailFilterRule addRemoveHeader(String headerName) {
		return addRemoveHeaders(Arrays.asList(headerName));
	}

	public MailFilterRule addRemoveHeaders(List<String> headerNames) {
		removeAction(MailFilterRuleActionName.REMOVE_HEADERS);
		actions.add(new MailFilterRuleActionRemoveHeaders(headerNames));
		return this;
	}

	public Optional<MailFilterRuleActionUncategorize> uncategorize() {
		return actions.stream() //
				.filter(action -> action.name.equals(MailFilterRuleActionName.UNCATEGORIZE)) //
				.map(action -> (MailFilterRuleActionUncategorize) action).findFirst();
	}

	public MailFilterRule addUncategorize() {
		removeAction(MailFilterRuleActionName.UNCATEGORIZE);
		actions.add(new MailFilterRuleActionUncategorize());
		return this;
	}

	public Optional<MailFilterRuleActionUnfollow> unfollow() {
		return actions.stream() //
				.filter(action -> action.name.equals(MailFilterRuleActionName.UNFOLLOW)) //
				.map(action -> (MailFilterRuleActionUnfollow) action).findFirst();
	}

	public MailFilterRule addUnfollow() {
		removeAction(MailFilterRuleActionName.UNFOLLOW);
		actions.add(new MailFilterRuleActionUnfollow());
		return this;
	}

	public Optional<MailFilterRuleActionDiscard> discard() {
		return actions.stream() //
				.filter(action -> action.name.equals(MailFilterRuleActionName.DISCARD)) //
				.map(action -> (MailFilterRuleActionDiscard) action).findFirst();
	}

	public MailFilterRule addDiscard() {
		removeAction(MailFilterRuleActionName.DISCARD);
		actions.add(new MailFilterRuleActionDiscard());
		return this;
	}

	public Optional<MailFilterRuleActionCopy> copy() {
		return actions.stream() //
				.filter(action -> action.name.equals(MailFilterRuleActionName.COPY)) //
				.map(action -> (MailFilterRuleActionCopy) action).findFirst();
	}

	public MailFilterRule addCopy(String destinationFolder) {
		removeAction(MailFilterRuleActionName.COPY);
		actions.add(new MailFilterRuleActionCopy(destinationFolder));
		return this;
	}

	public MailFilterRule addCopyFromString(String value) {
		removeAction(MailFilterRuleActionName.COPY);
		actions.add(MailFilterRuleActionCopy.fromString(value));
		return this;
	}

	public Optional<MailFilterRuleActionMove> move() {
		return actions.stream() //
				.filter(action -> action.name.equals(MailFilterRuleActionName.MOVE)) //
				.map(action -> (MailFilterRuleActionMove) action).findFirst();
	}

	public MailFilterRule addMove(String destinationFolder) {
		removeAction(MailFilterRuleActionName.MOVE);
		actions.add(new MailFilterRuleActionMove(destinationFolder));
		return this;
	}

	public MailFilterRule addMoveFromString(String value) {
		removeAction(MailFilterRuleActionName.MOVE);
		actions.add(MailFilterRuleActionMove.fromString(value));
		return this;
	}

	public Optional<MailFilterRuleActionTransfer> transfer() {
		return actions.stream() //
				.filter(action -> action.name.equals(MailFilterRuleActionName.TRANSFER)) //
				.map(action -> (MailFilterRuleActionTransfer) action).findFirst();
	}

	public MailFilterRule addTransfer(List<String> emails, boolean asAttachment, boolean keepCopy) {
		removeAction(MailFilterRuleActionName.TRANSFER);
		actions.add(new MailFilterRuleActionTransfer(emails, asAttachment, keepCopy));
		return this;
	}

	public Optional<MailFilterRuleActionRedirect> redirect() {
		return actions.stream() //
				.filter(action -> action.name.equals(MailFilterRuleActionName.REDIRECT)) //
				.map(action -> (MailFilterRuleActionRedirect) action).findFirst();
	}

	public MailFilterRule addRedirect(List<String> emails, boolean keepCopy) {
		removeAction(MailFilterRuleActionName.REDIRECT);
		actions.add(new MailFilterRuleActionRedirect(emails, keepCopy));
		return this;
	}

	public Optional<MailFilterRuleActionReply> reply() {
		return actions.stream() //
				.filter(action -> action.name.equals(MailFilterRuleActionName.REPLY)) //
				.map(action -> (MailFilterRuleActionReply) action).findFirst();
	}

	public MailFilterRule addReply(String subject, String plainBody, String htmlBody) {
		removeAction(MailFilterRuleActionName.REPLY);
		actions.add(new MailFilterRuleActionReply(subject, plainBody, htmlBody));
		return this;
	}

	@GwtIncompatible
	public boolean match(FieldValueProvider fieldProvider, ParameterValueProvider parameterProvider) {
		return MailFilterRuleCondition.match(conditions, fieldProvider, parameterProvider);
	}

	public static MailFilterRule copy(MailFilterRule toCopy) {
		MailFilterRule rule = new MailFilterRule();
		rule.client = toCopy.client;
		rule.type = toCopy.type;
		rule.trigger = toCopy.trigger;
		rule.deferred = toCopy.deferred;
		rule.active = toCopy.active;
		rule.name = toCopy.name;
		rule.conditions = new ArrayList<>(toCopy.conditions);
		rule.actions = new ArrayList<>(toCopy.actions);
		rule.stop = toCopy.stop;
		return rule;
	}

	@Override
	public int hashCode() {
		return Objects.hash(actions, active, conditions, name, stop, client, trigger, type, deferred);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		MailFilterRule other = (MailFilterRule) obj;
		return Objects.equals(actions, other.actions) && active == other.active
				&& Objects.equals(conditions, other.conditions) && Objects.equals(name, other.name)
				&& Objects.equals(client, other.client) && stop == other.stop && trigger == other.trigger
				&& type == other.type;
	}
}
