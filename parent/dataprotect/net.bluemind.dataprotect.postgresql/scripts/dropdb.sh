#!/bin/bash

echo "** Drop database ${db}"
su postgres -c "dropdb ${db}"
echo "** Drop database ${db} complete."
