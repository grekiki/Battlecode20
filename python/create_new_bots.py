"""
Script that will create new bots. The procedure is the following:
- open json of some bot configurations in /bot_configurations
- modify values by a bit
- copy grekiki25 bot from Battlecode20/src into another bot folder
- change konst.java configuration into the configuration of current bot
"""

import subprocess
import json
import random

from jinja2 import Template

DOUBLES = set([
    "dist_optrange_factor",
    "dist_factor",
    "hqf3",
    "hqf4",   
])

def load_constants(bot_name):
    """
    Loads the configurations of the `bot_name` and returns it as a dictionary.
    """

    with open(f'bot_configurations/{bot_name}.json', 'r') as infile:
        data = json.load(infile)
    return data

def modify_constants(constants, percentages=10):
    """
    Modifies current configurations. So far it changes each parameter 
    from -10% up to 10%.

    Returns a dictionary of modified values.
    """

    for key, value in constants.items():
        if key == 'private_key':
            constants[key] = random.randint(10**8, 10**9)
        elif key in DOUBLES:
            change = random.random() / 5 - 0.1
            constants[key] = value * (1 + change)
        else:
            constants[key] = random.randint(round(value*0.9), round(value*1.1))

    return constants

def create_new_bot(parent, generation, number):
    """
    Creates a new bot out of the bot `parent`.
    Generation parameter specifies to which generation will this bot belong and 
    the number parameter is used to distuingish between bots within the same generation.

    - Modifies bot constants
    - saves them into file bot_configurations/generation_{generation}_id_{number}
    - copies contents of ../src/grekiki25 bot into a new folder named 
        ..\srs\generation_{generation}_id_{number}
    - modifies `konst.java` file and updates constants with updated ones
    """

    parent_bot_configurations = load_constants(parent)
    child_bot_configurations = modify_constants(parent_bot_configurations, percentages=10)

    child_name = f'generation_{generation}_id_{number}'

    with open(f'bot_configurations/{child_name}.json', 'w') as outfile:
        json.dump(child_bot_configurations, outfile, indent=2)

    command = subprocess.run(f'xcopy /s ..\src\grekiki25 ..\src\generation_{generation}_id_{number}\\ /Y', shell=True)


    # Load template
    with open('konst_java_template.jinja2', 'r') as template_file:
        template = Template(template_file.read())

    with open(f'..\src\generation_{generation}_id_{number}\konst.java', 'w') as outfile:
        # print(template.render(child_bot_configurations), file=outfile)
        print(f'package {child_name};', file=outfile)
        print('public class konst {', file=outfile)
        for key, value in child_bot_configurations.items():
            if key in DOUBLES:
                print(f'public static double {key} = {value};', file=outfile)
            else:
                print(f'public static int {key} = {value};', file=outfile)
        print('}', file=outfile)
    
    # Change the package names in files
    for filename in ['buildings.java', 'pc.java', 'RobotPlayer.java', 'units.java']:
        with open(f'..\src\{child_name}\{filename}', 'r') as infile:
            lines = infile.readlines()
            lines[0] = f'package {child_name};'
        with open(f'..\src\{child_name}\{filename}', 'w') as outfile:
            for line in lines:
                print(line.rstrip('\n'), file=outfile)
        

if __name__ == '__main__':
    create_new_bot('grekiki25', 1, 15)