#!/bin/bash

RELAY=$(postconf -h relayhost )

if [ "$RELAY" ]; then
        echo "test"
fi

