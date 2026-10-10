package dev.gigglebyte.lanternembers;

import java.util.List;

/** Roles from the fix plan. A wrong role is told in the protect tone. */
final class RolePolicy {
  static final List<String> ROLES =
      List.of("lookout", "builder", "quartermaster", "storykeeper");
  static final List<String> FEEDS = List.of("full", "mentions", "summary");

  private RolePolicy() {}

  static boolean validRole(String role) {
    return ROLES.contains(role);
  }

  static boolean validFeed(String feed) {
    return FEEDS.contains(feed);
  }

  static boolean mayMark(String role) {
    return "lookout".equals(role);
  }

  static boolean mayPhrase(String role) {
    return "lookout".equals(role) || "storykeeper".equals(role);
  }

  static boolean mayConfirmMark(String role) {
    return "lookout".equals(role);
  }

  static boolean mayConfirmOffer(String role) {
    return "lookout".equals(role) || "storykeeper".equals(role);
  }

  static boolean mayOpenChest(String role) {
    return "quartermaster".equals(role);
  }

  static boolean mayBreak(
      String actor, String role, boolean inHour, String marker, boolean confirmed, boolean marked) {
    if (!marked) {
      return !"lookout".equals(role);
    }
    if (!confirmed) {
      return actor.equals(marker);
    }
    return actor.equals(marker) || ("builder".equals(role) && inHour);
  }

  static boolean mayPlace(
      String actor, String role, boolean inHour, String marker, boolean confirmed, boolean marked) {
    if (!marked) {
      return !"lookout".equals(role) && !"storykeeper".equals(role);
    }
    return mayBreak(actor, role, inHour, marker, confirmed, true);
  }
}
