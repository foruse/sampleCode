<?php
/**
 * Copyright © 2016 GBKSOFT. Web and Mobile Software Development.
 * See LICENSE.txt for license details.
 */
namespace gbksoft\multilingual\controllers\actions;

use gbksoft\multilingual\models\Language;
use yii\base\Action;
use yii\web\NotFoundHttpException;
use yii\web\Response;

/**
 * Class UpdateAction
 */
class UpdateAction extends Action
{
    /**
     * @param $id
     * @return string|Response
     * @throws NotFoundHttpException
     */
    public function run($id)
    {
        /** @var Language $model */
        $model = Language::findOne($id);
        if ($model === null) {
            throw new NotFoundHttpException('The requested language does not exist.');
        }

        if ($model->load(\Yii::$app->request->post()) && $model->save()) {

            return $this->controller->redirect(['update', 'id' => $model->id]);
        }

        return $this->controller->render('update', ['model' => $model]);
    }
}
