import cv2
import torch
import numpy as np
from sklearn.cluster import KMeans
import pandas as pd
from PIL import Image
import pathlib
from flask import Flask, request
import math


# development environment(개발 환경)
# conda python version: 3.9.19
# conda install --file packagelist.txt

# 리눅스, 맥 사용시 아래 코드 주석 처리 바람
# ____________________________________start
temp = pathlib.PosixPath
pathlib.PosixPath = pathlib.WindowsPath
# ____________________________________end


def extract_colors_from_image(image_path, Mymodele):
    # 모델 로드
    # model = torch.hub.load('ultralytics/yolov5', 'custom', path='best.pt')
    model = Mymodele
    # 이미지 로드
    img = cv2.imread(image_path)
    resize = (400, 500)

    # 원하는 크기로 이미지 리사이징 -> yolov5 권장 이미지 사이즈
    resized_image = cv2.resize(img, resize)

    # NumPy 배열을 PIL 이미지로 변환
    resized_image_pil = Image.fromarray(cv2.cvtColor(resized_image, cv2.COLOR_BGR2RGB))

    # 객체 검출 수행
    results_resized = model(resized_image_pil)

    # 바운딩 박스 좌표 추출!
    bboxes = [detection[0:4].cpu().numpy().astype(int) for detection in results_resized.xyxy[0]]

    # 바운딩 박스 좌표와 주요 색상 추출
    result_df = extract_colors_from_bboxes(resized_image_pil, bboxes)
    # # 결과 시각화
    # results_resized.print()  # 감지된 객체와 confidence scores 출력
    # results_resized.show()  # 이미지에 바운딩 박스와 클래스 라벨 표시
    # 추출된 주요 색상 출력
    print("Extracted Colors:")
    print(result_df)

    return result_df


def extract_colors_from_bboxes(resized_image, bboxes):
    result_df = pd.DataFrame(columns=['TOP_RGB', 'BOTTOM_RGB'])

    for idx, bbox in enumerate(bboxes):
        class_label = idx  # 예제에서는 클래스 레이블을 간단하게 인덱스로 사용합니다.
        # 주요 색상 추출
        cropped_image = resized_image.crop((bbox[0], bbox[1], bbox[2], bbox[3]))
        major_color = kmeans_find_major_color(cropped_image)

        # 주요 색상과 클래스 레이블 DataFrame 에 추가
        if major_color is not None:
            if class_label == 0:
                result_df.loc[0, 'TOP_RGB'] = major_color
            elif class_label == 1:
                result_df.loc[0, 'BOTTOM_RGB'] = major_color
        else:
            # 주요 색상이 없는 경우 출력하지 않음
            continue

    return result_df


def convert_to_original(data):
    return tuple(np.array(data) * 255.0)


def convert_to_numeric(data):
    # RGB 값들을 0에서 1 사이의 실수 값으로 정규화
    return np.array([np.array(rgb) / 255.0 for rgb in data])


def kmeans_find_major_color(image):
    pixels = np.array(image)[:, :, :3].reshape((-1, 3))
    kmeans = KMeans(n_clusters=1, n_init=10, random_state=42)
    kmeans.fit(pixels)

    if kmeans.cluster_centers_ is not None and len(kmeans.cluster_centers_) > 0:
        dominant_color = tuple(map(int, kmeans.cluster_centers_[0]))
        return dominant_color
    else:
        return None


# development environment(개발 환경)
# conda python version: 3.9.19
# conda install --file packagelist.txt
def penton_color(RGB, RGB2, num):
    if isinstance(RGB, tuple) and isinstance(RGB2, tuple):
        R1, G1, B1 = RGB
        R2, G2, B2 = RGB2
        R = (R1 - R2) ** 2
        G = (G1 - G2) ** 2
        B = (B1 - B2) ** 2
        if math.sqrt(R + G + B) < num:
            return True
    return False

# 걸러내고 싶은 함수
def filter_paths(row, TOP_RGB, BOTTOM_RGB):
    top_rgb = row['TOP_RGB']
    bottom_rgb = row['BOTTOM_RGB']
    imgPath = row['path']
    df = pd.DataFrame(columns=["path"])
    idx = 0
    for t, b, i in zip(top_rgb, bottom_rgb, imgPath):
        # 문자열을 튜플로 변환
        t = tuple(map(int, t.strip('()').split(','))) if isinstance(t, str) else t
        b = tuple(map(int, b.strip('()').split(','))) if isinstance(b, str) else b
        if penton_color(t, TOP_RGB, 45) and penton_color(b, BOTTOM_RGB,45):
            df.loc[idx] = i
            idx += 1
    if df['path'].empty:
        idx = 0
        for t, b, i in zip(top_rgb, bottom_rgb, imgPath):
            # 문자열을 튜플로 변환
            t = tuple(map(int, t.strip('()').split(','))) if isinstance(t, str) else t
            b = tuple(map(int, b.strip('()').split(','))) if isinstance(b, str) else b
            if penton_color(t, TOP_RGB, 45) and penton_color(b, BOTTOM_RGB,50):
                df.loc[idx] = i
                idx += 1
    return df

app = Flask(__name__)

model = torch.hub.load('ultralytics/yolov5', 'custom', path='best.pt')
@app.route('/api', methods=['GET'])
def api_call():
    uri = request.args.get('path')  # 사용자가 전달한 이미지 URI
    style = request.args.get('style')
    result = extract_colors_from_image(uri, model)  # 이미지에서 색상 추출
    # 추출된 주요 색상 데이터
    result_top = result['TOP_RGB'].values[0]
    result_bottom = result['BOTTOM_RGB'].values[0]
    # CSV 파일에서 색상 데이터 읽어오기
    df = pd.read_csv(f'che_csv/{style}.csv',  header=0, names=['TOP_RGB', 'BOTTOM_RGB', 'path'])
    print(df.info())
    # path 요소만 추출하는 함수 정의
    path = filter_paths(df, result_top, result_bottom)
    print(path.info())
    print(path.to_json(orient="records"))
    return path.to_json(orient="records")

if __name__ == '__main__':
    app.run(debug=True)
