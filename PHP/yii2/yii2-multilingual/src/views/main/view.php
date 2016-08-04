<?php
/**
 * Copyright © 2016 GBKSOFT. Web and Mobile Software Development.
 * See LICENSE.txt for license details.
 */

use gbksoft\multilingual\models\Language;
use yii\grid\ActionColumn;
use yii\grid\SerialColumn;
use yii\helpers\Html;
use yii\grid\GridView;
use yii\helpers\Url;
use yii\widgets\Pjax;

/**
 * @var yii\web\View $this
 * @var yii\data\ActiveDataProvider $dataProvider
 * @var gbksoft\multilingual\models\search\Language $searchModel
 */

$this->title = Yii::t('language-backend', 'Languages');
$this->params['breadcrumbs'][] = $this->title;

?>
<div class="role-index">
    <h1><?= Html::encode($this->title) ?></h1>
    <p>
        <?= Html::a(Yii::t('language-backend', 'Create Language'), ['create'], ['class' => 'btn btn-success']) ?>
    </p>
    <?php
    Pjax::begin([
        'enablePushState'=>false,
    ]);
    echo GridView::widget([
        'dataProvider' => $dataProvider,
        'filterModel' => $searchModel,
        'columns' => [
            ['class' => SerialColumn::class],
            [
                'attribute' => 'name',
                'label' => Yii::t('language-backend', 'Name'),
            ],
            [
                'attribute' => 'locale',
                'label' => Yii::t('language-backend', 'Locale'),
            ],
            [
                'attribute' => 'url',
                'label' => Yii::t('language-backend', 'Url'),
            ],
            [
                'class' => ActionColumn::class,
                'template' => '{update} {delete}',
                'urlCreator' => function ($action, Language $model) {
                    return Url::to([$action, 'id' => $model->id]);
                }
            ],
        ],
    ]);
    Pjax::end();
    ?>
</div>
