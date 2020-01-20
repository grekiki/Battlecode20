import subprocess
import os

def play_game(bot1, bot2, maps):
    """
    Implies we are running this command from Battlecode20/python folder. Executes command in shell that plays
    the game between `bot1` and `bot2` on map `maps`.
    """

    current_dir = os.getcwd()

    command = f'..\gradlew -p .. run ' + f'-PteamA={bot1} -PteamB={bot2} -Pmaps={maps}'
    result = subprocess.run(command, stdout=subprocess.PIPE, shell=True, cwd=current_dir)
    # print(result)
    decoded_match_string = result.stdout.decode('utf-8')
    # print(decoded_match_string)
    match_result = decoded_match_string[-500:]
    
    if '(A) wins' in match_result:
        return 'PLAYER 1 WINS'
    elif '(B) wins' in match_result:
        return 'PLAYER 2 WINS'
    else:
        print(result)
        return 'Something went wrong?'


if __name__ == '__main__':
    print(play_game('grekiki25_ai', 'grekiki3', 'FourLakeLand'))