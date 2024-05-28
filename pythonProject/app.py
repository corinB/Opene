import cv2
import torch
import numpy as np
from sklearn.cluster import KMeans
import pandas as pd
from PIL import Image
import pathlib
temp = pathlib.PosixPath
pathlib.PosixPath = pathlib.WindowsPath

def extract_colors_from_bboxes(resized_image, bboxes):
    result_df = pd.DataFrame(columns=['TOP_RGB', 'BOTTOM_RGB'])

    for idx, bbox in enumerate(bboxes):
        class_label = idx  # 예제에서는 클래스 레이블을 간단하게 인덱스로 사용합니다.

        # 주요 색상 추출
        cropped_image = resized_image.crop((bbox[0], bbox[1], bbox[2], bbox[3]))

        major_color = kmeans_find_major_color(cropped_image)

        # 주요 색상과 클래스 레이블을 DataFrame에 추가
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

# 모델 로드
model = torch.hub.load('ultralytics/yolov5', 'custom', path='best.pt')
# force_reload=True 안되면 옵션 추가


# 이미지 로드
img = cv2.imread('t1.webp'))
resize=(400, 500)

# 원하는 크기로 이미지 리사이징
resized_image = cv2.resize(img, resize)

# NumPy 배열을 PIL 이미지로 변환
resized_image_pil = Image.fromarray(cv2.cvtColor(resized_image, cv2.COLOR_BGR2RGB))

# 객체 검출 수행
results_resized = model(resized_image_pil)

# 바운딩 박스 좌표 추출
bboxes = [detection[0:4].cpu().numpy().astype(int) for detection in results_resized.xyxy[0]]

# 바운딩 박스 좌표와 주요 색상 추출
result_df = extract_colors_from_bboxes(resized_image_pil, bboxes)

# 결과 시각화
results_resized.print()  # 감지된 객체와 confidence scores 출력
results_resized.show()   # 이미지에 바운딩 박스와 클래스 라벨 표시

# 추출된 주요 색상 출력
print("Extracted Colors:")
print(result_df)
