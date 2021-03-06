/*
 * =========================================================================
 *  Copyright (c) 2002-2014 Pivotal Software, Inc. All Rights Reserved.
 *  This product is protected by U.S. and international copyright
 *  and intellectual property laws. Pivotal products are covered by
 *  more patents listed at http://www.pivotal.io/patents.
 * ========================================================================
 */

package com.gemstone.gemfire.internal.process.signal;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The AbstractSignalNotificationHandler class...
 * </p>
 * @author John Blum
 * @see com.gemstone.gemfire.internal.process.signal.Signal
 * @see com.gemstone.gemfire.internal.process.signal.SignalEvent
 * @see com.gemstone.gemfire.internal.process.signal.SignalListener
 * @since 7.0
 */
@SuppressWarnings("unused")
public abstract class AbstractSignalNotificationHandler {

  // NOTE use the enumerated type instead...
  @Deprecated
  protected static final List<String> SIGNAL_NAMES;

  // Based on Open BSD OS Signals...
  static {
    final String[] SIGNAL_NAMES_ARRAY = new String[] {
      "", "HUP", "INT", "QUIT", "ILL", "TRAP", "ABRT", "EMT", "FPE", "KILL", "BUS", "SEGV", "SYS", "PIPE", "ALRM",
      "TERM", "URG", "STOP", "TSTP", "CONT", "CHLD", "TTIN", "TTOU", "IO", "XCPU", "XFSZ", "VTALRM", "PROF", "WINCH",
      "INFO", "USR1", "USR2"
    };

    SIGNAL_NAMES = Collections.unmodifiableList(Arrays.asList(SIGNAL_NAMES_ARRAY));
  }

  protected static final SignalListener LOGGING_SIGNAL_LISTENER = new SignalListener() {
    public void handle(final SignalEvent event) {
      System.out.printf("Logging SignalListener Received Signal '%1$s' (%2$d)%n", event.getSignal().getName(),
        event.getSignal().getNumber());
    }
  };

  protected static final SignalListener NO_OP_SIGNAL_LISTENER = new SignalListener() {
    public void handle(final SignalEvent event) {
      // no op
    }
  };

  // Map used to register SignalListeners with SignalHandlers...
  private final Map<Signal, Set<SignalListener>> signalListeners = Collections.synchronizedMap(
    new HashMap<Signal, Set<SignalListener>>(Signal.values().length));

  protected static void assertNotNull(final Object obj, final String message, final Object... arguments) {
    if (obj == null) {
      throw new NullPointerException(String.format(message, arguments));
    }
  }

  protected static void assertState(final boolean state, final String message, final Object... arguments) {
    if (!state) {
      throw new IllegalStateException(String.format(message, arguments));
    }
  }

  protected static void assertValidArgument(final boolean valid, final String message, final Object... arguments) {
    if (!valid) {
      throw new IllegalArgumentException(String.format(message, arguments));
    }
  }

  public AbstractSignalNotificationHandler() {
    for (final Signal signal : Signal.values()) {
      signalListeners.put(signal, Collections.synchronizedSet(new HashSet<SignalListener>()));
    }
    // NOTE uncomment for debugging purposes...
    //registerListener(LOGGING_SIGNAL_LISTENER);
  }

  public boolean hasListeners(final Signal signal) {
    return !signalListeners.get(signal).isEmpty();
  }

  public boolean isListening(final SignalListener listener) {
    boolean registered = false;

    for (final Signal signal : Signal.values()) {
      registered |= isListening(listener, signal);
    }

    return registered;
  }

  public boolean isListening(final SignalListener listener, final Signal signal) {
    assertNotNull(signal, "The signal to determine whether the listener is registered listening for cannot be null!");
    return signalListeners.get(signal).contains(listener);
  }

  protected void notifyListeners(final SignalEvent event) {
    final Set<SignalListener> listeners = signalListeners.get(event.getSignal());
    Set<SignalListener> localListeners = Collections.emptySet();

    if (listeners != null) {
      synchronized (listeners) {
        localListeners = new HashSet<SignalListener>(listeners);
      }
    }

    for (final SignalListener listener : localListeners) {
      listener.handle(event);
    }
  }

  public boolean registerListener(final SignalListener listener) {
    assertNotNull(listener, "The SignalListener to register, listening for all signals cannot be null!");

    boolean registered = false;

    for (final Signal signal : Signal.values()) {
      registered |= registerListener(listener, signal);
    }

    return registered;
  }

  public boolean registerListener(final SignalListener listener, final Signal signal) {
    assertNotNull(signal, "The signal to register the listener for cannot be null!");
    assertNotNull(listener, "The SignalListener being registered to listen for '%1$s' signals cannot be null!",
      signal.getName());

    return signalListeners.get(signal).add(listener);
  }

  public boolean unregisterListener(final SignalListener listener) {
    boolean unregistered = false;

    for (final Signal signal : Signal.values()) {
      unregistered |= unregisterListener(listener, signal);
    }

    return unregistered;
  }

  public boolean unregisterListener(final SignalListener listener, final Signal signal) {
    assertNotNull(signal, "The signal from which to unregister the listener cannot be null!");

    return signalListeners.get(signal).remove(listener);
  }

  public boolean unregisterListeners(final Signal signal) {
    assertNotNull(signal, "The signal from which to unregister all listeners cannot be null!");

    final Set<SignalListener> listeners = signalListeners.get(signal);

    synchronized (listeners) {
      listeners.clear();
      return listeners.isEmpty();
    }
  }

}
