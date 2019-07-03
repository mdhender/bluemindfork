#!/usr/bin/env python

from __future__ import print_function
import sys, os.path, fnmatch, polib, codecs
from datetime import datetime

langs = ['de', 'en', 'es', 'fr', 'it', 'nl', 'pl', 'ru', 'sk', 'uk', 'zh']

podirname = "freemarker-templates"

def usage():
    print("Usage:")
    print('    "' + sys.argv[0] + 
          ' collect" to collect ftl templates into po files')
    print('    "' + sys.argv[0] + 
          ' export" to export the po files back into ftl templates')
    print('\nThis tool does NOT provide any merge strategy: use with caution!')


def keyFromPath(path):
    return path[path.find("plugins"): -7]


def collect(matches, podir):
    for l in langs:
        po = polib.POFile()
        po.metadata = {
            'Project-Id-Version': '1.0',
            'Report-Msgid-Bugs-To': 'dominique.eav@blue-mind.net',
            'POT-Creation-Date': datetime.now().isoformat(),
            'PO-Revision-Date': datetime.now().isoformat(),
            'MIME-Version': '1.0',
            'Content-Type': 'text/plain; charset=utf-8',
            'Content-Transfer-Encoding': '8bit',
        }
        for filepath in matches[l]:
            with codecs.open(filepath, 'r', 'utf-8') as f:
                entry = polib.POEntry(
                    msgid=keyFromPath(filepath),
                    msgstr=f.read()
                )
                f.close()
                po.append(entry)
        po.save(os.path.join(podir, "{}.po".format(l)))
    print("Collect completed")


def export(matches, podir):
    for l in langs:
        po = polib.pofile(os.path.join(podir, "{}.po".format(l)))
        translations = {}
        for entry in po:
            translations[entry.msgid] = entry.msgstr
        for match in matches[l]:
            with codecs.open(match, 'w', 'utf-8') as f:
                f.write(translations[keyFromPath(match)])
                f.close()
    print("Export completed.")


if __name__ == '__main__':
    if (len(sys.argv) != 2):
        usage()
        sys.exit()
    currentpath = os.path.dirname(__file__)
    podir = os.path.join(currentpath, podirname)
    matches = {}
    for l in langs:
        matches[l] = []
    for root, dirnames, filenames in os.walk(os.path.join(
            currentpath, '..', '..')):
        for l in langs: 
            for filename in fnmatch.filter(filenames, '*_{}.ftl'.format(l)):
                matches[l].append(os.path.join(root, filename))
    if (sys.argv[1] == 'collect'):
        collect(matches, podir)
    elif (sys.argv[1] == 'export'):
        export(matches, podir)
    else:
        usage()
    
    
