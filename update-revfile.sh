#!/bin/bash

function parse_git_branch {
  git branch --no-color 2> /dev/null | sed -e '/^[^*]/d' -e 's/* \(.*\)/\1/'
}

function get_git_revision {
  git rev-parse `parse_git_branch`
}


#echo "branch:" `parse_git_branch`
#echo "sha1:  " `get_git_revision`
REVNO=$(get_git_revision)
echo "package de.uvwxy.footpath;public class Rev{public final static String rev = \"$REVNO\";}" > gen/de/uvwxy/footpath/Rev.java



