"""Build a deterministic, wholly synthetic match for the public portfolio demo."""
from pathlib import Path
import csv
import io
import zipfile

out = Path(__file__).parent / 'portfolio-demo.zip'
files = {'match_demo.txt': 'SYNTHETIC-PORTFOLIO-DEMO\n'}

def table(name, rows):
    stream = io.StringIO(newline='')
    csv.writer(stream, lineterminator='\n').writerows(rows)
    files[name] = stream.getvalue()

roles = ['TOP', 'JUNGLE', 'MIDDLE', 'BOTTOM', 'UTILITY']
table('info.csv', [['team', 'player_id', 'summoner_name', 'position']] + [
    [team, index + offset, f'Demo{team}{index}', role]
    for team, offset in [('BLUE', 0), ('RED', 5)]
    for index, role in enumerate(roles, 1)])

pairs = [
    (1, '목표 위치를 확인할까', 0, '목표 위치를 확인했어'),
    (2, '다음 구간으로 이동하자', 3, '함께 이동할게'),
    (0, '위쪽 경로가 비어 있어', 0, '아래쪽 경로도 확인했어'),
    (0, '다음 목표까지 시간이 남았어', 1, '주변부터 살펴볼까'),
    (0, '상대가 멀리 이동했어', 2, '지금 위치를 정리하자'),
    (3, '목표 주변에서 기다릴게', 0, '나도 근처에 도착했어'),
]
voices = [['speaker', 'text', 'start_ms', 'end_ms', 'unused', 'act_code']]
for block in range(12):
    first, text1, second, text2 = pairs[block % len(pairs)]
    speaker1 = block % 5 + 1
    speaker2 = (block + 1) % 5 + 1
    for speaker, start, text, code in [
        (speaker1, block * 10000 + 1000, text1, first),
        (speaker2, block * 10000 + 2500, text2, second),
    ]:
        voices.append([f'{speaker}-demo', '합성 예시: ' + text, start, start + 1000, '', code])
table('da_result.csv', voices)
table('event.csv', [['event', 'time_ms', 'killer_id', 'victim_id'],
    ['CHAMPION_KILL', 18000, '1-demo', '6-demo'],
    ['CHAMPION_KILL', 48000, '7-demo', '2-demo'],
    ['CHAMPION_KILL', 78000, '3-demo', '8-demo'],
    ['CHAMPION_KILL', 120000, '4-demo', '9-demo']])

with zipfile.ZipFile(out, 'w', zipfile.ZIP_DEFLATED) as archive:
    for name, content in files.items():
        info = zipfile.ZipInfo(name, date_time=(2026, 9, 18, 0, 0, 0))
        info.compress_type = zipfile.ZIP_DEFLATED
        archive.writestr(info, content.encode('utf-8'))
print('Synthetic demo: 10 fictional players, 24 utterances, 4 events, 120 seconds.')
