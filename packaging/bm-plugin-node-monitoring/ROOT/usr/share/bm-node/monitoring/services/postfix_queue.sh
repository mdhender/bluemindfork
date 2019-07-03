#!/bin/sh

postqueue -p | tail -1 | awk '{print ($5 ? $5 : 0)}'
